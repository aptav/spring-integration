/*
 * Copyright 2002-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.integration.context.metadata;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.util.Assert;
import org.springframework.util.DefaultPropertiesPersister;

/**
 * Properties file-based implementation of {@link MetadataStore}. To avoid conflicts
 * each instance should be constructed with the unique key from which unique file name 
 * will be generated. The file name will be 'persistentKey' + ".last.entry".
 * Files will be written to the 'java.io.tmpdir' +  "/spring-integration/".
 * 
 * @author Oleg Zhurakousky
 * @since 2.0
 */
public class FileBasedPropertiesStore implements MetadataStore, InitializingBean{
	private final Log logger = LogFactory.getLog(getClass());
	private final DefaultPropertiesPersister persister = new DefaultPropertiesPersister();
	private final String persistentKey;
	private volatile File persistentFile;
	private volatile String baseDirectory = System.getProperty("java.io.tmpdir") + "/spring-integration/";

	public FileBasedPropertiesStore(String persistentKey){
		Assert.notNull(persistentKey, "'persistentKey' must not be null");
		this.persistentKey = persistentKey;
	}
	
	public void setBaseDirectory(String baseDirectory) {
		this.baseDirectory = baseDirectory;
	}
	
	public String getBaseDirectory() {
		return baseDirectory;
	}

	public void write(Properties metadata) {
		FileOutputStream fo = null;
		 try {
			fo = new FileOutputStream(persistentFile);
         	persister.store(metadata, fo, "Last feed entry");
 		} 
         catch (IOException e) {
         	// not fatal for the functionality of the component
 			logger.warn("Failed to persist feed entry. This may result in a duplicate " +
 					"feed entry after this component is restarted", e);
 		} 
         finally {
 			try {
 				if (fo != null){
 					fo.close();
 				}
 			} 
 			catch (IOException e) {
 				// not fatal for the functionality of he component
 				logger.warn("Failed to close FileOutputStream to " + persistentFile.getAbsolutePath(), e);
 			}
 		}
	}

	public Properties load() {
		Properties properties = new Properties();
		FileInputStream iStream = null;
		try {
			iStream = new FileInputStream(persistentFile);
			persister.load(properties, iStream);
		} catch (Exception e) {
			// not fatal for the functionality of the component
 			logger.warn("Failed to load feed entry from the persistent store. This may result in a duplicate " +
 					"feed entry after this component is restarted", e);
		} finally {
			try {
				if (iStream != null){
					iStream.close();
				}
			} catch (Exception e2) {
				// non fatal
				logger.warn("Failed to close FileInputStream for: " + persistentFile.getAbsolutePath());
			}
		}
		return properties;
	}

	public void afterPropertiesSet() throws Exception {
		String fileName = this.persistentKey + ".last.entry";
		File baseDir = new File(baseDirectory);
		baseDir.mkdirs();
		persistentFile = new File(baseDir, fileName);
		try {
			if (!persistentFile.exists()){
				persistentFile.createNewFile();
			}
		} catch (Exception e) {
			throw new IllegalArgumentException("Failed to create metadata-store file '" 
					+ persistentFile.getAbsolutePath() + "'", e);
		}
	}
}