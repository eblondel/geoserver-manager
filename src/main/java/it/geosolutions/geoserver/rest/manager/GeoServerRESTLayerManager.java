package it.geosolutions.geoserver.rest.manager;

import it.geosolutions.geoserver.rest.HTTPUtils;
import it.geosolutions.geoserver.rest.encoder.GSLayerEncoder;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Layer manager
 * 
 * @author Emmanuel Blondel (emmanuel.blondel at fao.org)
 *
 */
public class GeoServerRESTLayerManager extends GeoServerRESTAbstractManager {

	private final static Logger LOGGER = LoggerFactory.getLogger(GeoServerRESTLayerManager.class);
	
	/**
     * Default constructor.
     *
     * @param restURL GeoServer REST API endpoint
     * @param username GeoServer REST API authorized username
     * @param password GeoServer REST API password for the former username
     */
    public GeoServerRESTLayerManager(URL restURL, String username, String password)
            throws IllegalArgumentException {
        super(restURL, username, password);
    }

    /**
     * Allows to configure some layer attributes such as DefaultStyle
     * 
     * @param workspace
     * @param resourceName the name of the resource to use (featureStore or coverageStore name)
     * @param layer the layer encoder used to configure the layer
     * @return true if success
     * @throws IllegalArgumentException if some arguments are null or empty
     * 
     * @TODO WmsPath
     */
    public boolean configureLayer(final String workspace, final String resourceName,
            final GSLayerEncoder layer) throws IllegalArgumentException {

        if (workspace == null || resourceName == null || layer == null) {
            throw new IllegalArgumentException("Null argument");
        }
        // TODO: check this usecase, layer should always be defined
        if (workspace.isEmpty() || resourceName.isEmpty() || layer.isEmpty()) {
            throw new IllegalArgumentException("Empty argument");
        }

        final String fqLayerName = workspace + ":" + resourceName;

        final String url = gsBaseUrl.toString() + "/rest/layers/" + fqLayerName;

        String layerXml = layer.toString();
        String sendResult = HTTPUtils.putXml(url, layerXml, gsuser, gspass);
        if (sendResult != null) {
            if (LOGGER.isInfoEnabled()) {
                LOGGER.info("Layer successfully configured: " + fqLayerName);
            }
        } else {
            if (LOGGER.isWarnEnabled())
                LOGGER.warn("Error configuring layer " + fqLayerName + " (" + sendResult + ")");
        }

        return sendResult != null;
    }


}
