/*
 *  OpenSDI Manager 2
 *  Copyright (C) 2014 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 *
 *  GPLv3 + Classpath exception
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package it.geosolutions.opensdi2.workflow.action;

import it.geosolutions.opensdi2.workflow.BaseAction;
import it.geosolutions.opensdi2.workflow.BlockConfiguration;
import it.geosolutions.opensdi2.workflow.WorkflowContext;
import it.geosolutions.opensdi2.workflow.WorkflowException;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;

import org.opengis.feature.simple.SimpleFeature;

/**
 * Updates an input {@link SimpleFeature} properties adding:
 *  - Username
 *  - Date
 * If the input feature isn't a {@link SimpleFeature} throw a IllegalArgumentException.
 * Throws {@link WorkflowException} if {@link IOException} occurs.
 * 
 * @author Lorenzo Pini (lorenzo.pini@geo-solutions.it)
 */
public class MetadataUpdater extends BaseAction {
	private String inputId = "input";
	private DataStoreConfiguration dataStoreConfiguration;
	
	@Override
	public void executeAction(WorkflowContext ctx) throws WorkflowException {
		
		Object inputObj = ctx.getContextElement(inputId);
		if(inputObj != null && inputObj instanceof SimpleFeature) {
			SimpleFeature feature = (SimpleFeature)inputObj;
			feature.setAttribute("DATA_AGG", new SimpleDateFormat("dd/MMMM/yyyy").format(new Date()));
			feature.setAttribute("NOME_RILEVATORE", ctx.getContextElement("USERNAME"));
			
			ctx.addContextElement(inputId, feature);
		} else {
			throw new IllegalArgumentException("Invalid input: must be a SimpleFeature object");
		}
	}

	@Override
	public void setConfiguration(BlockConfiguration config) {
		super.setConfiguration(config);
		if(config != null && config instanceof DataStoreConfiguration) {
			dataStoreConfiguration = (DataStoreConfiguration)config;
			if(dataStoreConfiguration.getInputObjectId() != null) {
				inputId = dataStoreConfiguration.getInputObjectId();
			}
		}
	}

	public Map<String, String> getAttributeMappings() {
		//return attributeMappings;
		return null;
	}

	public void setAttributeMappings(Map<String, String> attributeMappings) {
		//this.attributeMappings = attributeMappings;
	}
	
	

}
