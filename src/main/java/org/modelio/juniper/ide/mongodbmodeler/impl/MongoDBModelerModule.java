/**
 * Copyright 2011 Modeliosoft
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modelio.juniper.ide.mongodbmodeler.impl;

import java.io.File;

import javax.script.ScriptEngine;
import javax.script.ScriptException;

import org.modelio.api.model.IModelingSession;
import org.modelio.api.module.AbstractJavaModule;
import org.modelio.api.module.IParameterEditionModel;
import org.modelio.api.module.IModuleAPIConfiguration;
import org.modelio.api.module.IModuleSession;
import org.modelio.api.module.IModuleUserConfiguration;
import org.modelio.juniper.ide.mongodbmodeler.audit.rules.MonitoringRules;
import org.modelio.metamodel.mda.ModuleComponent;
import org.modelio.modelvalidator.ModelValidatorFacade;
import org.modelio.modelvalidator.engine.IModelValidator;

/**
 * Implementation of the IModule interface. <br>
 * All Modelio java modules should inherit from this class.
 * 
 */
public class MongoDBModelerModule extends AbstractJavaModule {

	private MongoDBModelerPeerModule peerModule = null;

	private MongoDBModelerSession session = null;

	@Override
	public MongoDBModelerPeerModule getPeerModule() {
		return this.peerModule;
	}

	/**
	 * Return the session attached to the current module.
	 * <p>
	 * <p>
	 * This session is used to manage the module lifecycle by declaring the
	 * desired implementation on start, select... methods.
	 */
	@Override
	public IModuleSession getSession() {
		return this.session;
	}

	/**
	 * Method automatically called just after the creation of the module.
	 * <p>
	 * <p>
	 * The module is automatically instanciated at the beginning of the MDA
	 * lifecycle and constructor implementation is not accessible to the module
	 * developer.
	 * <p>
	 * <p>
	 * The <code>init</code> method allows the developer to execute the desired
	 * initialization code at this step. For example, this is the perfect place
	 * to register any IViewpoint this module provides.
	 *
	 *
	 * @see org.modelio.api.module.AbstractJavaModule#init()
	 */
	@Override
	public void init() {
		// Add the module initialization code
		super.init();
		// installing validation rules
		installValidationRules();

		// setting default classpath for scripts importing
		String path = this.getConfiguration().getModuleResourcesPath()
				.toString()
				+ "/res/scripts/";
		ScriptEngine jythonEngine = this.getJythonEngine();
		try {
			jythonEngine.put("scriptsPath", new File(path).getAbsolutePath());
			jythonEngine.eval("sys.path.append(scriptsPath)");
		} catch (ScriptException e) {
			e.printStackTrace();
		}
	}

	private void installValidationRules() {
		IModelValidator modelValidator = ModelValidatorFacade.getInstance()
				.getModelValidator();

		MonitoringRules.init(modelValidator);
	}

	@Override
	public void uninit() {
		// Add the module un-initialization code
		super.uninit();
	}

	public MongoDBModelerModule(IModelingSession modelingSession,
			ModuleComponent moduleComponent,
			IModuleUserConfiguration moduleConfiguration,
			IModuleAPIConfiguration peerConfiguration) {
		super(modelingSession, moduleComponent, moduleConfiguration);
		this.session = new MongoDBModelerSession(this);
		this.peerModule = new MongoDBModelerPeerModule(this, peerConfiguration);
		this.peerModule.init();
		// this.propertyPages.add(new RequirementPropertyPage(this,
		// "RequirementPropertyPage", "RequirementPropertyPage",
		// "res/icons/edit.png"));
	}

	/**
	 * @see org.modelio.api.module.AbstractJavaModule#getParametersEditionModel()
	 */
	@Override
	public IParameterEditionModel getParametersEditionModel() {
		if (this.parameterEditionModel == null) {
			this.parameterEditionModel = super.getParametersEditionModel();
		}
		return this.parameterEditionModel;
	}

	@Override
	public String getModuleImagePath() {
		return "/res/icons/favicon.ico";
	}

}
