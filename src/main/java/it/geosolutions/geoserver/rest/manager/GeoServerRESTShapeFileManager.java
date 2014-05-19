package it.geosolutions.geoserver.rest.manager;

import it.geosolutions.geoserver.rest.GeoServerRESTPublisher.UploadMethod;
import it.geosolutions.geoserver.rest.encoder.GSLayerEncoder;
import it.geosolutions.geoserver.rest.encoder.GSResourceEncoder.ProjectionPolicy;
import it.geosolutions.geoserver.rest.encoder.datastore.GSShapefileDatastoreEncoder;
import it.geosolutions.geoserver.rest.encoder.feature.GSFeatureTypeEncoder;

import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import org.apache.commons.httpclient.NameValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * ShapeFile Manager
 * 
 * @author Emmanuel Blondel (emmanuel.blondel at fao.org)
 *
 */
public class GeoServerRESTShapeFileManager extends GeoServerRESTAbstractManager {

	private final static Logger LOGGER = LoggerFactory.getLogger(GeoServerRESTShapeFileManager.class);
	
	private GeoServerRESTStoreManager storeManager;
	private GeoServerRESTResourceManager resourceManager;
	private GeoServerRESTLayerManager layerManager;
	
	/**
	 * Default constructor.
	 * 
	 * @param restURL
	 *            GeoServer REST API endpoint
	 * @param username
	 *            GeoServer REST API authorized username
	 * @param password
	 *            GeoServer REST API password for the former username
	 */
	public GeoServerRESTShapeFileManager(URL restURL, String username,
			String password) throws IllegalArgumentException {
		super(restURL, username, password);
		
		//required managers
		storeManager = new GeoServerRESTStoreManager(restURL, username, password);
		resourceManager = new GeoServerRESTResourceManager(restURL, username, password);
		layerManager = new GeoServerRESTLayerManager(restURL, username, password);
	}
    
	/**
	 * Advanced Method to publish a shapefile, by relying GSFeatureTypeEncoder
	 * and GSLayerEncoder
	 * 
	 * @param workspace
	 * @param storeName
	 * @param storeParams
	 * @param datasetName
	 * @param method
	 * @param shapefile
	 * @param featureTypeEncoder
	 * @param layerEncoder
	 * @return true if success false otherwise
	 * 
	 * @throws FileNotFoundException
	 * @throws IllegalArgumentException
	 * @throws MalformedURLException 
	 */
	public boolean publishShp(String workspace, String storeName,
			NameValuePair[] storeParams, String datasetName,
			UploadMethod method, URI shapefile,
			GSFeatureTypeEncoder featureTypeEncoder, GSLayerEncoder layerEncoder)
			throws FileNotFoundException, IllegalArgumentException, MalformedURLException {

		String srs = featureTypeEncoder.getSRS();
		String nativeCRS = featureTypeEncoder.getNativeCRS();
		ProjectionPolicy policy = ProjectionPolicy.valueOf(featureTypeEncoder
				.getProjectionPolicy());

		if (workspace == null || storeName == null || shapefile == null
				|| datasetName == null
				|| featureTypeEncoder.getProjectionPolicy() == null) {
			throw new IllegalArgumentException("Unable to run: null parameter");
		}

		//
		// SRS Policy Management
		//
		boolean srsNull = !(srs != null && srs.length() != 0);
		boolean nativeSrsNull = !(nativeCRS != null && nativeCRS.length() != 0);
		// if we are asking to use the reproject policy we must have the native
		// crs
		if (policy == ProjectionPolicy.REPROJECT_TO_DECLARED
				&& (nativeSrsNull || srsNull)) {
			throw new IllegalArgumentException(
					"Unable to run: you can't ask GeoServer to reproject while not specifying a native CRS");
		}

		// if we are asking to use the NONE policy we must have the native crs.
		if (policy == ProjectionPolicy.NONE && nativeSrsNull) {
			throw new IllegalArgumentException(
					"Unable to run: you can't ask GeoServer to use a native srs which is null");
		}

		// if we are asking to use the reproject policy we must have the native
		// crs
		if (policy == ProjectionPolicy.FORCE_DECLARED && srsNull) {
			throw new IllegalArgumentException(
					"Unable to run: you can't force GeoServer to use an srs which is null");
		}

		//
		final String mimeType;
		switch (method) {
		case EXTERNAL:
		case external:
			mimeType = "text/plain";
			break;
		case URL: // TODO check which mime-type should be used
		case FILE:
		case file:
		case url:
			mimeType = "application/zip";
			break;
		default:
			mimeType = null;
		}

		//try to create datastore
		GSShapefileDatastoreEncoder shapefileStoreEncoder = new GSShapefileDatastoreEncoder(
				mimeType, shapefile.toURL());
		if (!storeManager.create(workspace, shapefileStoreEncoder)) {
			LOGGER.error("Unable to create data store for shapefile: "
					+ shapefile + "(Datastore might already exist)");
		}

		// set destination srs
		if (srsNull) {
			featureTypeEncoder.setSRS(nativeCRS);
		}

		if (!resourceManager.createFeatureType(workspace, storeName, featureTypeEncoder)) {
			LOGGER.error("Unable to create a coverage store for coverage: "
					+ shapefile);
			return false;
		}

		return layerManager.configureLayer(workspace, datasetName, layerEncoder);
	}
    
	
}
