package it.geosolutions.geoserver.rest.manager;

import it.geosolutions.geoserver.rest.HTTPUtils;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher.Format;
import it.geosolutions.geoserver.rest.GeoServerRESTPublisher.StoreType;
import it.geosolutions.geoserver.rest.encoder.GSLayerEncoder;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder;
import it.geosolutions.geoserver.rest.encoder.coverage.GSCoverageEncoder;
import it.geosolutions.geoserver.rest.encoder.feature.GSFeatureTypeEncoder;

import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resource (FeatureType/Coverage) manager
 * 
 * @author Emmanuel Blondel (emmanuel.blondel at fao.org)
 *
 */
public class GeoServerRESTResourceManager extends GeoServerRESTAbstractManager {

	private final static Logger LOGGER = LoggerFactory.getLogger(GeoServerRESTResourceManager.class);
	
	/**
     * Default constructor.
     *
     * @param restURL GeoServer REST API endpoint
     * @param username GeoServer REST API authorized username
     * @param password GeoServer REST API password for the former username
     */
    public GeoServerRESTResourceManager(URL restURL, String username, String password)
            throws IllegalArgumentException {
        super(restURL, username, password);
    }
    

    /**
     * Create a new resource in a given workspace and store
     * 
     * @param wsname the workspace to search for existent coverage
     * @param storeName an existent store name to use as data source
     * @param re contains the coverage name to create and the configuration to apply
     * 
     * @TODO For FeatureType: The list parameter is used to control the category of feature types that are returned. It can take one of the three
     *       values configured, available, or all.
     * 
     *       configured - Only setup or configured feature types are returned. This is the default value. available - Only unconfigured feature types
     *       (not yet setup) but are available from the specified datastore will be returned. available_with_geom - Same as available but only
     *       includes feature types that have a geometry granule. all - The union of configured and available.
     * 
     * 
     * @return true if success
     * @throws IllegalArgumentException if arguments are null or empty
     */
    private boolean createResource(String workspace, StoreType dsType, String storeName,
            GSResourceEncoder re) throws IllegalArgumentException {
        if (workspace == null || dsType == null || storeName == null || re == null) {
            throw new IllegalArgumentException("Null argument");
        }
        StringBuilder sbUrl = new StringBuilder(this.gsBaseUrl.toString()).append("/rest/workspaces/")
                .append(workspace).append("/").append(dsType).append("/").append(storeName)
                .append("/").append(dsType.getTypeNameWithFormat(Format.XML));

        final String resourceName = re.getName();
        if (resourceName == null) {
            throw new IllegalArgumentException(
                    "Unable to configure a coverage using unnamed coverage encoder");
        }

        final String xmlBody = re.toString();
        final String sendResult = HTTPUtils.postXml(sbUrl.toString(), xmlBody, gsuser, gspass);
        if (sendResult != null) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug(dsType + " successfully created " + workspace + ":" + storeName + ":"
                        + resourceName);
            }
        } else {
            if (LOGGER.isErrorEnabled())
                LOGGER.error("Error creating coverage " + workspace + ":" + storeName + ":"
                        + resourceName + " (" + sendResult + ")");
        }

        return sendResult != null;
    }
  
    /**
     * Create a new featureType in a given workspace and coverage store
     * 
     * @param wsname the workspace to search for existent featureType
     * @param storeName an existent store name to use as data source
     * @param ce contains the featureType name to create and the configuration to apply
     * @return true if success
     * @throws IllegalArgumentException if arguments are null or empty
     */
    public boolean createFeatureType(final String wsname, final String storeName,
            final GSFeatureTypeEncoder ce) throws IllegalArgumentException {
        return createResource(wsname, StoreType.DATASTORES, storeName, ce);
    }
    
    /**
     * Create a new coverage in a given workspace and coverage store
     * 
     * @param wsname the workspace to search for existent coverage
     * @param storeName an existent store name to use as data source
     * @param ce contains the coverage name to create and the configuration to apply
     * @return true if success
     * @throws IllegalArgumentException if arguments are null or empty
     */
    public boolean createCoverage(final String wsname, final String storeName,
            final GSCoverageEncoder ce) throws IllegalArgumentException {
        return createResource(wsname, StoreType.COVERAGESTORES, storeName, ce);
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
