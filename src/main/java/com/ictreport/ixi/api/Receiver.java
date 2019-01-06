package com.ictreport.ixi.api;

import com.ictreport.ixi.exchange.*;
import com.ictreport.ixi.utils.Constants;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;

import com.ictreport.ixi.model.Neighbor;
import org.iota.ict.ixi.ReportIxi;

public class Receiver extends Thread {

    private final static Logger LOGGER = LogManager.getLogger(Receiver.class);
    private final ReportIxi reportIxi;
    private final DatagramSocket socket;
    private boolean isReceiving = false;

    public Receiver(final ReportIxi reportIxi, final DatagramSocket socket) {
        super("Receiver");

        this.reportIxi = reportIxi;
        this.socket = socket;
    }

    @Override
    public void run() {
        isReceiving = true;

        while (isReceiving) {
            final byte[] buf = new byte[1024];
            final DatagramPacket packet = new DatagramPacket(buf, buf.length);

            try {
                socket.receive(packet);
                processPacket(packet);
            } catch (final IOException e) {
                if (isReceiving)
                    e.printStackTrace();
            }
        }
    }

    public void processPayload(final Neighbor neighbor, final Payload payload) {
        if (payload instanceof MetadataPayload) {
            processMetadataPacket(neighbor, (MetadataPayload) payload);
        } else if (payload instanceof SignedPayload) {
            processSignedPayload((SignedPayload) payload);
        } else if (payload instanceof UuidPayload) {
            processUuidPayload((UuidPayload) payload);
        }
    }

    public void shutDown() {
        isReceiving = false;
    }

    private void processPacket(final DatagramPacket packet) {
        Neighbor neighbor = determineNeighborWhoSent(packet);
        if (neighbor == null && !isPacketSentFromRCS(packet)) {
            LOGGER.warn("Received packet from unknown address: " + packet.getAddress());
            Metrics.incrementNonNeighborInvalidCount();
            return;
        }

        String data = new String(packet.getData(), 0, packet.getLength());

        try {
            final Payload payload = Payload.deserialize(data);
            processPayload(neighbor, payload);
        } catch (final Exception e) {
            if (neighbor != null) {
                LOGGER.info(String.format("Received invalid packet from Neighbor[%s]",
                        neighbor.getSocketAddress()));
            } else {
                LOGGER.info(String.format("Received invalid packet from RCS"));
            }
            e.printStackTrace();
        }
    }

    private void processUuidPayload(final UuidPayload uuidPayload) {
        reportIxi.setUuid(uuidPayload.getUuid());
        LOGGER.info(String.format("Received uuid from RCS"));
        synchronized (reportIxi.waitingForUuid) {
            reportIxi.waitingForUuid.notify();
        }
    }

    private void processMetadataPacket(final Neighbor neighbor, final MetadataPayload metadataPayload) {
        if (neighbor.getReportIxiVersion() == null ||
                !neighbor.getReportIxiVersion().equals(metadataPayload.getReportIxiVersion())) {
            neighbor.setReportIxiVersion(metadataPayload.getReportIxiVersion());
            LOGGER.info(String.format("Neighbor[%s] operates Report.ixi version: %s",
                    neighbor.getSocketAddress(),
                    neighbor.getReportIxiVersion()));
        }

        if (neighbor.getUuid() == null ||
                !neighbor.getUuid().equals(metadataPayload.getUuid())) {
            neighbor.setUuid(metadataPayload.getUuid());
            LOGGER.info(String.format("Received new uuid from neighbor[%s]",
                    neighbor.getSocketAddress()));
        }

        if (neighbor.getPublicKey() == null ||
                !neighbor.getPublicKey().equals(metadataPayload.getPublicKey())) {
            neighbor.setPublicKey(metadataPayload.getPublicKey());
            LOGGER.info(String.format("Received new publicKey from neighbor[%s]",
                    neighbor.getSocketAddress()));
        }

        neighbor.incrementMetadataCount();
    }

    private boolean isPacketSentFromRCS(final DatagramPacket packet) {
        try {
            final boolean sameIP = InetAddress.getByName(Constants.RCS_HOST).getHostAddress()
                    .equals(packet.getAddress().getHostAddress());
            final boolean samePort = Constants.RCS_PORT == packet.getPort();
            return sameIP && samePort;
        } catch (final UnknownHostException e) {
            return false;
        }
    }

    private void processSignedPayload(final SignedPayload signedPayload) {
        Neighbor signee = null;
        for (Neighbor neighbor : reportIxi.getNeighbors()) {
            if (neighbor.getPublicKey() != null && signedPayload.verify(neighbor.getPublicKey())) {
                signee = neighbor;
                break;
            }
        }

        if (signedPayload.getPayload() instanceof PingPayload) {
            final PingPayload pingPayload = (PingPayload) signedPayload.getPayload();
            final ReceivedPingPayload receivedPingPayload;
            if (signee != null) {
                receivedPingPayload = new ReceivedPingPayload(reportIxi.getUuid(),
                        pingPayload, true);
                signee.incrementPingCount();
            } else {
                receivedPingPayload = new ReceivedPingPayload(reportIxi.getUuid(),
                        pingPayload, false);
                Metrics.incrementNonNeighborPingCount();
            }
            if (reportIxi.getUuid() != null) {
                reportIxi.getApi().getSender().send(receivedPingPayload, Constants.RCS_HOST, Constants.RCS_PORT);
            }
        } else if (signedPayload.getPayload() instanceof SilentPingPayload) {
            if (signee != null) {
                signee.incrementPingCount();
            } else {
                Metrics.incrementNonNeighborPingCount();
            }
        }
    }

    private Neighbor determineNeighborWhoSent(final DatagramPacket packet) {
        for (final Neighbor neighbor : reportIxi.getNeighbors())
            if (neighbor.sentPacket(packet))
                return neighbor;
        for (final Neighbor neighbor : reportIxi.getNeighbors())
            if (neighbor.sentPacketFromSameIP(packet))
                return neighbor;
        LOGGER.warn("Received packet from unknown address: " + packet.getAddress());
        Metrics.incrementNonNeighborInvalidCount();
        return null;
    }
}
