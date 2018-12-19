package com.ictreport.ixi.utils;

import java.net.InetSocketAddress;
import java.util.InvalidPropertiesFormatException;
import java.util.LinkedList;
import java.util.List;

import com.ictreport.ixi.model.Neighbor;

public class Properties {

    final static String CONST_NAME = "name";
    final static String CONST_REPORT_PORT = "reportPort";
    final static String[] CONST_NEIGHBOR_REPORT_PORTS = { "neighborReportPortA", "neighborReportPortB", "neighborReportPortC" };

    private String name = null;
    private String uuid = null;
    private String host = "localhost";
    private int reportPort = 1338;
    private int ictPort = 1337;

    public Properties() {

    }  

    /**
     * @return the ictPort
     */
    public int getIctPort() {
        return ictPort;
    }

    /**
     * @param ictPort the ictPort to set
     */
    public void setIctPort(int ictPort) {
        this.ictPort = ictPort;
    }

    /**
     * @return the reportPort
     */
    public int getReportPort() {
        return reportPort;
    }

    /**
     * @param reportPort the reportPort to set
     */
    public void setReportPort(int reportPort) {
        this.reportPort = reportPort;
    }

    /**
     * @return the host
     */
    public String getHost() {
        return host;
    }

    /**
     * @param host the host to set
     */
    public void setHost(String host) {
        this.host = host;
    }

    /**
     * @return the uuid
     */
    public String getUuid() {
        return uuid;
    }

    /**
     * @param uuid the uuid to set
     */
    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }

    public void loadFromIctProperties(org.iota.ict.utils.Properties ictProperties, List<Neighbor> neighbors) {
        setHost(ictProperties.host);
        setIctPort(ictProperties.port);

        for(InetSocketAddress ictNeighbor : ictProperties.neighbors){
            neighbors.add(new Neighbor(ictNeighbor.getAddress(), ictNeighbor.getPort()));
        }
    }

    public void loadFromReportProperties(java.util.Properties reportProperties, List<Neighbor> neighbors)
            throws InvalidPropertiesFormatException {
        String name = reportProperties.getProperty(CONST_NAME, "").trim();
        if (!name.matches(".+\\s\\(ict-\\d+\\)")) {
            throw createNewInvalidPropertiesFormatException(CONST_NAME, "Please follow the naming convention: \"<name> (ict-<number>)\"");
        }
        setName(name);

        int reportPort;
        try {
            reportPort = Integer.parseInt(reportProperties.getProperty(CONST_REPORT_PORT, "1338"));
        } catch (NumberFormatException exception) {
            throw createNewInvalidPropertiesFormatException(CONST_REPORT_PORT);
        }
        if (reportPort <= 0) {
            throw createNewInvalidPropertiesFormatException(CONST_REPORT_PORT);
        }
        setReportPort(reportPort);

        for (int i = 0; i < neighbors.size(); i++) {
            try {
                int neighborPort = Integer.parseInt(reportProperties.getProperty(CONST_NEIGHBOR_REPORT_PORTS[i], "1338"));
                if (neighborPort <= 0) {
                    throw createNewInvalidPropertiesFormatException(CONST_NEIGHBOR_REPORT_PORTS[i]);
                }
                neighbors.get(i).setReportPort(neighborPort);
            } catch (NumberFormatException exception) {
                throw createNewInvalidPropertiesFormatException(CONST_NEIGHBOR_REPORT_PORTS[i]);
            }
        }
    }

    private InvalidPropertiesFormatException createNewInvalidPropertiesFormatException(String property) {
        return createNewInvalidPropertiesFormatException(property, null);
    }

    private InvalidPropertiesFormatException createNewInvalidPropertiesFormatException(String property, String errorMessage) {
        return new InvalidPropertiesFormatException(
            property + "-property in report.ixi.cfg incorrectly formatted." + (errorMessage != null ? errorMessage : ""));
    }


}