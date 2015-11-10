/**
 * Copyright 2014 Modeliosoft
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

package org.modelio.juniper.ide.mongodbmodeler.command.explorer;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.emf.common.util.EList;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.modelio.api.modelio.Modelio;
import org.modelio.api.module.IModule;
import org.modelio.api.module.IModuleAPIConfiguration;
import org.modelio.api.module.IPeerModule;
import org.modelio.api.module.commands.DefaultModuleCommandHandler;
import org.modelio.metamodel.uml.infrastructure.Dependency;
import org.modelio.metamodel.uml.infrastructure.ModelElement;
import org.modelio.metamodel.uml.infrastructure.ModelTree;
import org.modelio.metamodel.uml.statik.BindableInstance;
import org.modelio.metamodel.uml.statik.Instance;
import org.modelio.metamodel.uml.statik.NameSpace;
import org.modelio.metamodel.uml.statik.Package;
import org.modelio.vcore.smkernel.mapi.MObject;

public class GenerateSchemaInitializationScriptCommand extends
		DefaultModuleCommandHandler {

	@Override
	public boolean accept(List<MObject> selectedElements, IModule module) {

		return super.accept(selectedElements, module)
				&& selectedElements.size() == 1
				&& (((ModelElement) selectedElements.get(0)).isStereotyped(
						"JuniperIDE", "JUNIPERModel"));
	}

	@Override
	public void actionPerformed(List<MObject> selectedElements, IModule module) {

		IPeerModule jdesigner = Modelio.getInstance().getModuleService()
				.getPeerModule("JavaDesigner");

		IModuleAPIConfiguration config = jdesigner.getConfiguration();
		String fileLocation = config.getProjectSpacePath().toString()
				+ "\\deploymentScripts\\" + selectedElements.get(0).getName();
		new File(fileLocation + "\\").mkdirs();

		List<ModelTree> packages = ((Package) selectedElements.get(0))
				.getOwnedElement();

		List<NameSpace> databases = new ArrayList<NameSpace>();

		BufferedWriter bwScript = null;
		try {

			bwScript = new BufferedWriter(new FileWriter(fileLocation
					+ "\\deploymentCompositionScript.sh"));

			for (ModelTree _package : packages) {
				if (_package.isStereotyped("JuniperIDE",
						"SoftwareArchitectureModel")) {

					List<? extends MObject> els = _package
							.getCompositionChildren();

					for (MObject el : els) {
						if (((ModelElement) el).isStereotyped("MongoDBModeler",
								"MongoDBServer")) {

							EList<Dependency> dependencies = ((ModelElement) el)
									.getDependsOnDependency();

							for (Dependency dependency : dependencies) {

								if (dependency.isStereotyped("JuniperIDE",
										"Stores")) {

									NameSpace datamodel = (NameSpace) dependency
											.getDependsOn();
									if (datamodel.isStereotyped(
											"PersistentProfile", "DataModel")) {
										for (MObject database : datamodel
												.getCompositionChildren()) {
											if (((ModelElement) database)
													.isStereotyped(
															"MongoDBModeler",
															"Database")) {
												databases
														.add((NameSpace) database);
											}
										}
									}

								}
							}

							editConfigFile(el, databases, bwScript,
									fileLocation);
						}

					}

				}
			}

			bwScript.close();
		} catch (IOException e) {

			e.printStackTrace();
		}

		messageBox();
	}

	private void editConfigFile(MObject server, List<NameSpace> databases,
			BufferedWriter bwScript, String fileLocation) throws IOException {

		String createIndex = "";
		String configFile = "";

		String ipServer = "";
		String serverConfig = "";

		if (((NameSpace) server).getRepresenting().size() > 1) {
			for (Instance el : ((NameSpace) server).getRepresenting()) {
				if (el.isStereotyped("MongoDBModeler", "Router")) {
					serverConfig = "router";
					ipServer = ((BindableInstance) el).getCluster()
							.getTagValue("JuniperIDE", "ip");

					for (MObject database : databases) {
						configFile += "sh.enableSharding(\""
								+ database.getName() + "\")\n";

						for (MObject collection : database
								.getCompositionChildren()) {

							for (MObject collectionChild : collection
									.getCompositionChildren()) {

								if (((ModelElement) collectionChild)
										.isStereotyped("MongoDBModeler",
												"ShardingKey")) {
									configFile += "sh.shardCollection(\""
											+ database.getName() + "."
											+ collection.getName() + "\", { ";

									int numberOfKeys = 0;
									String splitAt = "";
									List<Dependency> dependencies = (List<Dependency>) ((ModelElement) collectionChild)
											.getDependsOnDependency()
											.stream()
											.filter(dep -> !dep.isStereotyped(
													"ModelerModule", "trace"))
											.collect(Collectors.toList());

									// dependencies =
									// sortDependencies(dependencies);
									System.out
											.println("Apres --------------- "
													+ dependencies
													+ " ---------------");
									;
									for (Dependency shardKey : dependencies) {
										if (numberOfKeys < dependencies.size() - 1) {
											configFile += "\""
													+ shardKey.getDependsOn()
															.getName()
													+ "\": 1,";
											if (shardKey
													.getTagValue(
															"MongoDBModeler",
															"splitAt") != null) {
												String description = shardKey
														.getTagValue(
																"MongoDBModeler",
																"splitAt");

												if (splitAt.equals("")) {
													splitAt += "\""
															+ shardKey
																	.getDependsOn()
																	.getName()
															+ "\":\""
															+ description
															+ "\"";
												} else {
													splitAt += ",\""
															+ shardKey
																	.getDependsOn()
																	.getName()
															+ "\":\""
															+ description
															+ "\"";
												}

											}
										} else {
											configFile += "\""
													+ shardKey.getDependsOn()
															.getName()
													+ "\": 1 })\n";

											if (shardKey
													.getTagValue(
															"MongoDBModeler",
															"splitAt") != null) {
												String description = shardKey
														.getTagValue(
																"MongoDBModeler",
																"splitAt");
												if (!splitAt.equals("")) {
													splitAt += ",";
												}
												splitAt += "\""
														+ shardKey
																.getDependsOn()
																.getName()
														+ "\":\"" + description;

											}
											if (!splitAt.equals("")) {
												configFile += "sh.splitAt(\""
														+ database.getName()
														+ "."
														+ collection.getName()
														+ "\",{" + splitAt
														+ "})\n";

											}

										}
										numberOfKeys++;

									}

								} else if (((ModelElement) collectionChild)
										.isStereotyped("MongoDBModeler",
												"Index")) {
									configFile = editConfigFileString(
											configFile, database, collection,
											collectionChild, createIndex);

								}
							}

						}
					}
				} else if (el.isStereotyped("MongoDBModeler", "Primary")) {
					serverConfig = "primary";
					ipServer = ((BindableInstance) el).getCluster()
							.getTagValue("JuniperIDE", "ip");
					for (MObject database : databases) {
						for (MObject collection : database
								.getCompositionChildren()) {

							for (MObject collectionChild : collection
									.getCompositionChildren()) {

								if (((ModelElement) collectionChild)
										.isStereotyped("MongoDBModeler",
												"Index")) {
									configFile = editConfigFileString(
											serverConfig, database, database,
											database, serverConfig);

								}
							}

						}

					}
				}
			}
		} else {

			for (Instance el : ((NameSpace) server).getRepresenting()) {
				serverConfig = "simpleMongoDB";
				ipServer = ((BindableInstance) el).getCluster().getTagValue(
						"JuniperIDE", "ip");

				for (MObject database : databases) {

					for (MObject collection : database.getCompositionChildren()) {

						for (MObject collectionChild : collection
								.getCompositionChildren()) {

							if (((ModelElement) collectionChild).isStereotyped(
									"MongoDBModeler", "Index")) {
								configFile = editConfigFileString(configFile,
										database, collection, collectionChild,
										createIndex);

							}
						}

					}

				}

			}

		}

		editScriptDeployConfig(bwScript, fileLocation, configFile,
				serverConfig, ipServer);
	}

	private String editConfigFileString(String configFile, MObject database,
			MObject collection, MObject collectionChild, String createIndex) {
		configFile += "use " + database.getName() + "\ndb."
				+ collection.getName() + ".ensureIndex( { ";

		int nbIndex = 0;
		List<Dependency> dependencies = (List<Dependency>) ((ModelElement) collectionChild)
				.getDependsOnDependency().stream()
				.filter(dep -> !dep.isStereotyped("ModelerModule", "trace"))
				.collect(Collectors.toList());
		for (Dependency index : dependencies) {
			if (nbIndex < dependencies.size() - 1) {
				if (index.getTagValue("MongoDBModeler", "description") != null) {

					String description = index.getTagValue("MongoDBModeler",
							"description");
					if (!description.equals("1") && !description.equals("-1")) {
						createIndex += index.getDependsOn().getName() + ": \""
								+ description + "\", ";
					} else {
						createIndex += index.getDependsOn().getName() + ": "
								+ description + ", ";
					}
				} else {
					createIndex += index.getDependsOn().getName() + ": 1, ";
				}
			} else {
				if (index.getTagValue("MongoDBModeler", "description") != null) {
					String description = index.getTagValue("MongoDBModeler",
							"description");
					if (!description.equals("1") && !description.equals("-1")) {
						createIndex += index.getDependsOn().getName() + ": \""
								+ description + "\"})\n";
					} else {
						createIndex += index.getDependsOn().getName() + ": "
								+ description + " })\n";
					}

				} else {
					createIndex += index.getDependsOn().getName() + ": 1 } )\n";
				}

				configFile += createIndex;
			}
			nbIndex++;
		}

		return configFile;
	}

	private void editScriptDeployConfig(BufferedWriter bwScript,
			String fileLocation, String configFile, String server,
			String ipServer) throws IOException {

		File dossier = new File(fileLocation + "\\" + server + ipServer);

		if (!dossier.exists() || !dossier.isDirectory()) {
			new File(fileLocation + "\\" + server + ipServer).mkdirs();
		}
		BufferedWriter bwConfigFile = new BufferedWriter(new FileWriter(
				fileLocation + "\\" + server + ipServer
						+ "\\dataConfigFile.txt"));
		bwConfigFile.write(configFile);

		bwConfigFile.close();

		bwScript.write("scp " + server + ipServer + "/dataConfigFile.txt"
				+ " root@" + ipServer + ":./" + server + ipServer
				+ "\nssh root@" + ipServer + " \" cd " + server + ipServer
				+ "; mongo < ./dataConfigFile.txt \"\n");

	}

	private void messageBox() {

		MessageBox msg = new MessageBox(Display.getCurrent().getActiveShell(),
				SWT.ICON_INFORMATION | SWT.OK);
		msg.setMessage("Code generated succesfully");
		msg.open();
	}
}
