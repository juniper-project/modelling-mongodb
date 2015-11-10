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
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.MessageBox;
import org.modelio.api.modelio.Modelio;
import org.modelio.api.module.IModule;
import org.modelio.api.module.IModuleAPIConfiguration;
import org.modelio.api.module.IPeerModule;
import org.modelio.api.module.commands.DefaultModuleCommandHandler;
import org.modelio.metamodel.uml.infrastructure.ModelElement;
import org.modelio.metamodel.uml.infrastructure.ModelTree;
import org.modelio.metamodel.uml.statik.BindableInstance;
import org.modelio.metamodel.uml.statik.Instance;
import org.modelio.metamodel.uml.statik.Package;
import org.modelio.vcore.smkernel.mapi.MObject;

public class GenerateDeploymentScriptCommand extends
		DefaultModuleCommandHandler {

	@Override
	public boolean accept(List<MObject> selectedElements, IModule module) {
		return super.accept(selectedElements, module) && selectedElements.size() == 1
				&& (((ModelElement) selectedElements.get(0)).isStereotyped(
						"JuniperIDE", "JUNIPERModel"));
	}

	@Override
	public void actionPerformed(List<MObject> selectedElements, IModule module) {
		String modulePath = module.getConfiguration().getModuleResourcesPath()
				.toString();
		IPeerModule jdesigner = Modelio.getInstance().getModuleService()
				.getPeerModule("JavaDesigner");

		IModuleAPIConfiguration config = jdesigner.getConfiguration();

		String fileLocation = config.getProjectSpacePath().toString()
				+ "\\deploymentScripts\\" + selectedElements.get(0).getName();
		new File(fileLocation + "\\").mkdirs();

		Map<String, String> primaries = new HashMap<String, String>();
		List<BindableInstance> secondaries = new ArrayList<BindableInstance>();
		Map<String, String> arbiters = new HashMap<String, String>();
		Map<BindableInstance, String> shards = new HashMap<BindableInstance, String>();
		List<BindableInstance> shardSets = new ArrayList<BindableInstance>();
		List<BindableInstance> configServers = new ArrayList<BindableInstance>();
		Map<String, String> routers = new HashMap<String, String>();

		Map<String, List<String>> infoSecondary = new HashMap<String, List<String>>();
		Map<String, Map<String, String>> infoShards = new HashMap<String, Map<String, String>>();
		Map<String, List<String>> infoConfigServers = new HashMap<String, List<String>>();

		List<ModelTree> packages = ((Package) selectedElements.get(0))
				.getOwnedElement();

		for (ModelTree packag : packages) {
			if (packag.isStereotyped("JuniperIDE", "HardwarePlatformModel")) {
				List<Instance> nodes = ((Package) packag).getDeclared();

				BufferedWriter bwScript = null;
				try {

					bwScript = new BufferedWriter(new FileWriter(fileLocation
							+ "\\deploymentScript.sh"));

					for (Instance node : nodes) {

						List<BindableInstance> els = node.getPart();

						for (BindableInstance el : els) {

							if (el.isStereotyped("JuniperIDE",
									"ProgramInstance")) {
								if (el.getBase().isStereotyped(
										"MongoDBModeler", "MongoDBServer")) {

									if (el.isStereotyped("MongoDBModeler",
											"Primary")) {
										primaries.put(
												el.getBase().getName(),
												el.getCluster().getTagValue(
														"JuniperIDE", "ip"));
										infoSecondary.put(el.getBase()
												.getName(),
												new ArrayList<String>());

										if (el.isStereotyped("MongoDBModeler",
												"Shard")) {
											shards.put(el, el.getBase()
													.getName());
											shardSets.add(el);
										}

									} else if (el.isStereotyped(
											"MongoDBModeler", "Secondary")) {

										secondaries.add(el);

										if (el.isStereotyped("MongoDBModeler",
												"Shard")) {
											shards.put(el, el.getBase()
													.getName());
											shardSets.add(el);
										}

									} else if (el.isStereotyped(
											"MongoDBModeler", "Arbiter")) {
										arbiters.put(
												el.getBase().getName(),
												el.getCluster().getTagValue(
														"JuniperIDE", "ip"));

									} else if (el.isStereotyped(
											"MongoDBModeler", "Shard")) {
										shards.put(el, null);
										shardSets.add(el);

									} else if (el.isStereotyped(
											"MongoDBModeler", "Router")) {
										routers.put(
												el.getBase().getName(),
												el.getCluster().getTagValue(
														"JuniperIDE", "ip"));
										infoShards.put(el.getBase().getName(),
												new HashMap<String, String>());
										infoConfigServers.put(el.getBase()
												.getName(),
												new ArrayList<String>());
									} else if (el.isStereotyped(
											"MongoDBModeler", "ConfigServer")) {
										configServers.add(el);

									} else {
										editSimpleDeploymentScript(
												bwScript,
												fileLocation,
												modulePath,
												el.getCluster().getTagValue(
														"JuniperIDE", "ip"));

									}
								}
							}
						}

					}

					for (BindableInstance secondary : secondaries) {
						infoSecondary.get(secondary.getBase().getName()).add(
								secondary.getCluster().getTagValue(
										"JuniperIDE", "ip"));
					}

					for (BindableInstance shard : shardSets) {
						infoShards.get(shard.getBase().getName()).put(
								shard.getCluster().getTagValue("JuniperIDE",
										"ip"), shards.get(shard));

					}

					for (BindableInstance configServer : configServers) {
						infoConfigServers.get(configServer.getBase().getName())
								.add(configServer.getCluster().getTagValue(
										"JuniperIDE", "ip"));
					}

					for (Entry<String, String> primary : primaries.entrySet()) {
						editReplicatDeploymentScript(bwScript, fileLocation,
								modulePath, primary.getKey(),
								primary.getValue(),
								infoSecondary.get(primary.getKey()));
					}

					for (Entry<String, String> router : routers.entrySet()) {
						editShardDeploymentScript(bwScript, fileLocation,
								modulePath, router.getValue(),
								infoShards.get(router.getKey()),
								infoConfigServers.get(router.getKey()));
					}

					bwScript.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
				messageBox();
			}
		}
	}

	public void editSimpleDeploymentScript(BufferedWriter bwScript,
			String fileLocation, String modulePath, String ip) {

		try {
			new File(fileLocation + "\\simpleMongoDB" + ip).mkdirs();

			File to = new File(fileLocation + "\\simpleMongoDB" + ip
					+ "\\installMongoDB.sh");

			File from = new File(modulePath + "/res/mongodb/installMongoDB.sh");

			Files.copy(from.toPath(), to.toPath());

			File to2 = new File(fileLocation + "\\simpleMongoDB" + ip
					+ "\\script_needs.sh");
			File from2 = new File(modulePath + "/res/mongodb/script_needs.sh");
			Files.copy(from2.toPath(), to2.toPath());

			bwScript.write("scp -r simpleMongoDB"
					+ ip
					+ " root@"
					+ ip
					+ ":.\n ssh root@"
					+ ip
					+ " \" cd simpleMongoDB"
					+ ip
					+ "; bash ./script_needs.sh ; bash ./installMongoDB.sh \"\n");

		} catch (IOException e) {

			e.printStackTrace();
		}

	}

	private void editShardDeploymentScript(BufferedWriter bwScript,
			String fileLocation, String modulePath, String routerIP,
			Map<String, String> infoShards, List<String> configServersIP) {

		try {

			String fileAddShardConfig = "";

			for (Entry<String, String> shard : infoShards.entrySet()) {
				if (shard.getValue() == null) {
					new File(fileLocation + "\\shard" + shard.getKey())
							.mkdirs();

					File toshard = new File(fileLocation + "\\shard"
							+ shard.getKey() + "\\script_needs.sh");

					File fromshard = new File(modulePath
							+ "/res/mongodb/script_needs.sh");

					Files.copy(fromshard.toPath(), toshard.toPath());

					File toshardsimple = new File(fileLocation + "\\shard"
							+ shard.getKey() + "\\installMongoDB.sh");

					File fromshardsimple = new File(modulePath
							+ "/res/mongodb/installMongoDB.sh");

					Files.copy(fromshardsimple.toPath(), toshardsimple.toPath());

					bwScript.write("scp -r shard"
							+ shard.getKey()
							+ " root@"
							+ shard.getKey()
							+ ":.\n ssh root@"
							+ shard.getKey()
							+ " \"cd shard"
							+ shard.getKey()
							+ "; bash ./script_needs.sh ; bash ./installMongoDB.sh\"\n");

					fileAddShardConfig += "sh.addShard(\"" + shard.getKey()
							+ "\")\n";
				} else {

					fileAddShardConfig += "sh.addShard(\"" + shard.getValue()
							+ "/" + shard.getKey() + "\")\n";
				}

			}

			String configServers = "";

			for (String configServerIP : configServersIP) {

				new File(fileLocation + "\\configServer" + configServerIP)
						.mkdirs();

				File to = new File(fileLocation + "\\configServer"
						+ configServerIP + "\\mongodbConfigServer.sh");

				File from = new File(modulePath
						+ "/res/mongodb/mongodbConfigServer.sh");

				Files.copy(from.toPath(), to.toPath());

				File to2 = new File(fileLocation + "\\configServer"
						+ configServerIP + "\\script_needs.sh");

				File from2 = new File(modulePath
						+ "/res/mongodb/script_needs.sh");

				Files.copy(from2.toPath(), to2.toPath());

				bwScript.write("scp -r configServer"
						+ configServerIP
						+ " root@"
						+ configServerIP
						+ ":.\n ssh root@"
						+ configServerIP
						+ " \"cd configServer"
						+ configServerIP
						+ "; bash ./script_needs.sh ; bash ./mongodbConfigServer.sh\"\n");

				configServers += configServerIP + " ";
			}

			new File(fileLocation + "\\router" + routerIP).mkdirs();

			File to = new File(fileLocation + "\\router" + routerIP
					+ "\\mongodbRouter.sh");

			File from = new File(modulePath + "/res/mongodb/mongodbRouter.sh");

			Files.copy(from.toPath(), to.toPath());

			File to2 = new File(fileLocation + "\\router" + routerIP
					+ "\\script_needs.sh");

			File from2 = new File(modulePath + "/res/mongodb/script_needs.sh");

			Files.copy(from2.toPath(), to2.toPath());

			BufferedWriter bwAddShardConfig = new BufferedWriter(
					new FileWriter(fileLocation + "\\router" + routerIP
							+ "\\addShardConfig.txt"));
			bwAddShardConfig.write(fileAddShardConfig);

			bwAddShardConfig.close();

			bwScript.write("scp -r router" + routerIP + " root@" + routerIP
					+ ":.\n ssh root@" + routerIP + " \"cd router" + routerIP
					+ "; bash ./script_needs.sh ; bash ./mongodbRouter.sh "
					+ configServers + "\"\n");

		} catch (IOException e) {

			e.printStackTrace();
		}

	}

	private void editReplicatDeploymentScript(BufferedWriter bwScript,
			String fileLocation, String modulePath, String replicatSetName,
			String primaryIP, List<String> secondaryIPs) {

		try {
			String launchSecondary = "";
			int nbMembers = 0;
			String fileConfig = "config = {_id: '" + replicatSetName
					+ "',members:[{_id: " + nbMembers + ", host: '" + primaryIP
					+ ":27017'},";

			for (String secondaryIP : secondaryIPs) {
				nbMembers++;

				new File(fileLocation + "\\secondary" + secondaryIP).mkdirs();
				File tosecondary2 = new File(fileLocation + "\\secondary"
						+ secondaryIP + "\\script_needs.sh");

				File fromsecondary2 = new File(modulePath
						+ "/res/mongodb/script_needs.sh");

				Files.copy(fromsecondary2.toPath(), tosecondary2.toPath());

				File tosecondary = new File(fileLocation + "\\secondary"
						+ secondaryIP + "\\mongoDBReplicationSlave.sh");

				File fromsecondary = new File(modulePath
						+ "/res/mongodb/mongoDBReplicationSlave.sh");

				Files.copy(fromsecondary.toPath(), tosecondary.toPath());

				File tosecondary3 = new File(fileLocation + "\\secondary"
						+ secondaryIP + "\\launchReplicationSlave.sh");

				File fromsecondary3 = new File(modulePath
						+ "/res/mongodb/launchReplicationSlave.sh");

				Files.copy(fromsecondary3.toPath(), tosecondary3.toPath());

				bwScript.write("scp -r secondary"
						+ secondaryIP
						+ " root@"
						+ secondaryIP
						+ ":.\n ssh root@"
						+ secondaryIP
						+ " \"cd secondary"
						+ secondaryIP
						+ "; bash ./script_needs.sh ; bash ./mongoDBReplicationSlave.sh "
						+ replicatSetName + "\"\n");
				if (nbMembers < secondaryIPs.size() - 1) {
					fileConfig += "{_id: " + nbMembers + ", host: '"
							+ secondaryIP + ":27017'},";
				} else {
					fileConfig += "{_id: " + nbMembers + ", host: '"
							+ secondaryIP + ":27017'}]}\nrs.initiate(config)";
				}

				launchSecondary += "ssh root@" + secondaryIP
						+ " \"cd secondary" + secondaryIP
						+ "; bash ./launchReplicationSlave.sh\"\n";
			}

			new File(fileLocation + "\\primary" + primaryIP).mkdirs();

			File toprimary2 = new File(fileLocation + "\\primary" + primaryIP
					+ "\\script_needs.sh");

			File fromprimary2 = new File(modulePath
					+ "/res/mongodb/script_needs.sh");

			Files.copy(fromprimary2.toPath(), toprimary2.toPath());

			File toprimary = new File(fileLocation + "\\primary" + primaryIP
					+ "\\mongoDBReplicationMaster.sh");

			File fromprimary = new File(modulePath
					+ "/res/mongodb/mongoDBReplicationMaster.sh");

			Files.copy(fromprimary.toPath(), toprimary.toPath());

			BufferedWriter bwConfig = new BufferedWriter(new FileWriter(
					fileLocation + "\\primary" + primaryIP + "\\config.txt"));
			bwConfig.write(fileConfig);

			bwConfig.close();

			bwScript.write("scp -r primary"
					+ primaryIP
					+ " root@"
					+ primaryIP
					+ ":.\n ssh root@"
					+ primaryIP
					+ " \"cd primary"
					+ primaryIP
					+ "; bash ./script_needs.sh ; bash ./mongoDBReplicationMaster.sh "
					+ replicatSetName + "\"\n");

			bwScript.write(launchSecondary);

		} catch (IOException e) {

			e.printStackTrace();
		}

	}

	private void messageBox() {

		MessageBox msg = new MessageBox(Display.getCurrent().getActiveShell(),
				SWT.ICON_INFORMATION | SWT.OK);
		msg.setMessage("Code generated succesfully");
		msg.open();
	}

}
