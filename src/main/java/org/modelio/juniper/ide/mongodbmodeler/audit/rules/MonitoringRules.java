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
package org.modelio.juniper.ide.mongodbmodeler.audit.rules;

import java.util.List;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.TreeIterator;
import org.eclipse.emf.ecore.EObject;
import org.modelio.metamodel.uml.infrastructure.Dependency;
import org.modelio.metamodel.uml.infrastructure.ModelElement;
import org.modelio.metamodel.uml.statik.Instance;
import org.modelio.metamodel.uml.statik.Package;
import org.modelio.metamodel.uml.statik.NameSpace;
import org.modelio.modelvalidator.engine.IModelValidator;
import org.modelio.modelvalidator.engine.impl.AbstractSimplifiedRule;
import org.modelio.temp.audit.service.AuditSeverity;
import org.modelio.vcore.smkernel.mapi.MObject;

public class MonitoringRules {
	public static void init(IModelValidator modelValidator) {
		modelValidator.registerRule(
				new AbstractSimplifiedRule("MONGO_0008a",
						org.modelio.metamodel.uml.statik.Class.class) {
					@Override
					public boolean check(MObject obj, List<Object> linked) {
						MObject el = obj;
						if (((ModelElement) el).isStereotyped("MongoDBModeler",
								"MongoDBServer")) {
							List<Instance> representing = ((NameSpace) el)
									.getRepresenting();
							if (representing.size() > 1) {
								for (Instance represent : representing) {
									if (represent.isStereotyped(
											"MongoDBModeler", "Shard")
											|| represent.isStereotyped(
													"MongoDBModeler", "Router")
											|| represent.isStereotyped(
													"MongoDBModeler",
													"ConfigServer")) {

										EList<Dependency> dependencies = ((ModelElement) el)
												.getDependsOnDependency();

										for (Dependency dependency : dependencies) {

											if (dependency.isStereotyped(
													"JuniperIDE", "Stores")) {

												NameSpace datamodel = (NameSpace) dependency
														.getDependsOn();
												TreeIterator<EObject> it = datamodel
														.eAllContents();
												while (it.hasNext()) {
													if (((ModelElement) it
															.next())
															.isStereotyped(
																	"MongoDBModeler",
																	"ShardingKey")) {
														return true;
													}
												}

											}

										}
										return false;
									}
								}
							}

						}
						return true;
					}
				},
				AuditSeverity.AuditWarning,
				"Shardset architecture should have a ShardingKey in the data model.",
				"help");

		modelValidator.registerRule(
				new AbstractSimplifiedRule("MONGO_008b",
						org.modelio.metamodel.uml.statik.Class.class) {

					@Override
					public boolean check(MObject obj, List<Object> linked) {
						MObject el = obj;
						if (((ModelElement) el).isStereotyped("MongoDBModeler",
								"ShardingKey")) {
							ModelElement mongoServer = ((ModelElement) ((NameSpace) el)
									.getCompositionOwner()
									.getCompositionOwner()
									.getCompositionOwner())
									.getImpactedDependency().get(0)
									.getImpacted();
							if (((NameSpace) mongoServer).getRepresenting()
									.size() <= 1) {
								return false;
							} else {
								for (Instance represent : ((NameSpace) mongoServer)
										.getRepresenting()) {
									if (represent.isStereotyped(
											"MongoDBModeler", "Shard")
											|| represent.isStereotyped(
													"MongoDBModeler", "Router")
											|| represent.isStereotyped(
													"MongoDBModeler",
													"ConfigServer")) {
										return true;

									} else {
										return false;
									}
								}
							}
						}
						return true;
					}
				}, AuditSeverity.AuditError,
				"No shardingKey if the architecture is not a shardSet mode",
				"help");

		modelValidator.registerRule(
				new AbstractSimplifiedRule("MONGO_009a",
						org.modelio.metamodel.uml.statik.Class.class) {

					@Override
					public boolean check(MObject obj, List<Object> linked) {
						ModelElement el = (ModelElement) obj;
						if (el.isStereotyped("MongoDBModeler", "MongoDBServer")) {
							if (((NameSpace) el).getRepresenting().size() > 1) {
								boolean replicatSet = false;
								boolean shardset = false;
								boolean single = false;
								for (Instance represent : ((NameSpace) el)
										.getRepresenting()) {
									if (represent.isStereotyped(
											"MongoDBModeler", "Shard")
											|| represent.isStereotyped(
													"MongoDBModeler", "Router")
											|| represent.isStereotyped(
													"MongoDBModeler",
													"ConfigServer")) {
										shardset = true;
									} else if (represent.isStereotyped(
											"MongoDBModeler", "Primary")
											|| represent.isStereotyped(
													"MongoDBModeler",
													"Secondary")) {
										replicatSet = true;
									} else if (represent.isStereotyped(
											"MongoDBModeler", "Single")) {
										single = true;
									}

								}
								if ((replicatSet && shardset)
										|| (replicatSet && single)
										|| (shardset && single)
										|| (replicatSet && shardset && single)) {
									return false;
								}
							}
						}
						return true;
					}
				},
				AuditSeverity.AuditError,
				"MongoDB server should be represented by one type of architecture.",
				"help");

		modelValidator.registerRule(
				new AbstractSimplifiedRule("MONGO_009b",
						org.modelio.metamodel.uml.statik.Class.class) {

					@Override
					public boolean check(MObject obj, List<Object> linked) {
						ModelElement el = (ModelElement) obj;
						if (el.isStereotyped("MongoDBModeler", "MongoDBServer")) {
							if (((NameSpace) el).getRepresenting().size() > 1) {

								int nbShard = 0;
								int nbRouter = 0;
								int nbConfigserver = 0;
								for (Instance represent : ((NameSpace) el)
										.getRepresenting()) {
									if (represent.isStereotyped(
											"MongoDBModeler", "Shard")) {
										nbShard++;
									} else if (represent.isStereotyped(
											"MongoDBModeler", "Router")) {
										nbRouter++;
									} else if (represent.isStereotyped(
											"MongoDBModeler", "ConfigServer")) {
										nbConfigserver++;
									}

								}
								return (nbShard >= 1
										&& (nbConfigserver == 1 || nbConfigserver == 3)
										&& nbRouter == 1) || (nbConfigserver==0 && nbRouter==0 && nbShard==0);

							}
						}

						return true;
					}
				},
				AuditSeverity.AuditError,
				"An architecture shardSet should have one router and at least one shard and one or three configServer.",
				"help");

		modelValidator.registerRule(
				new AbstractSimplifiedRule("MONGO_009c",
						org.modelio.metamodel.uml.statik.Class.class) {

					@Override
					public boolean check(MObject obj, List<Object> linked) {
						ModelElement el = (ModelElement) obj;
						if (el.isStereotyped("MongoDBModeler", "MongoDBServer")) {
							if (((NameSpace) el).getRepresenting().size() > 1) {

								int nbPrimary = 0;
								int nbSecondary = 0;
								for (Instance represent : ((NameSpace) el)
										.getRepresenting()) {
									if (represent.isStereotyped(
											"MongoDBModeler", "Primary")) {
										nbPrimary++;
									} else if (represent.isStereotyped(
											"MongoDBModeler", "Secondary")) {
										nbSecondary++;
									}
								}
								return (nbPrimary == 1 && nbSecondary >= 1) || (nbPrimary==0 && nbSecondary==0);

							}
						}

						return true;
					}
				},
				AuditSeverity.AuditError,
				"An architecture replicatSet should have one primary and at least one secondary.",
				"help");

		modelValidator.registerRule(
				new AbstractSimplifiedRule("MONGO_009d",
						org.modelio.metamodel.uml.statik.Class.class) {

					@Override
					public boolean check(MObject obj, List<Object> linked) {
						ModelElement el = (ModelElement) obj;
						if (el.isStereotyped("MongoDBModeler", "MongoDBServer")) {
							if (((NameSpace) el).getRepresenting().size() < 1) {
								return false;
							}
						}

						return true;
					}
				},
				AuditSeverity.AuditError,
				"A MongoDBServer should have either a single instance or replicated instances or shard instances .",
				"help");

		modelValidator.registerRule(new AbstractSimplifiedRule("MONGO_010a",
				org.modelio.metamodel.uml.statik.Package.class) {
			@Override
			public boolean check(MObject obj, List<Object> linked) {
				ModelElement el = (ModelElement) obj;
				if (el.isStereotyped("MongoDBModeler", "Database")) {

					return el.isStereotyped("PersistentProfile", "DataModel");
				}
				return true;
			}
		}, AuditSeverity.AuditWarning,
				"The package Database should be stereotyped DataModel", "help");

		modelValidator.registerRule(
				new AbstractSimplifiedRule("MONGO_010b",
						org.modelio.metamodel.uml.statik.Package.class) {
					@Override
					public boolean check(MObject obj, List<Object> linked) {
						ModelElement el = (ModelElement) obj;
						if (el.isStereotyped("MongoDBModeler", "Database")) {
							if (((NameSpace) el).getOwner().isStereotyped(
									"PersistentProfile", "DataModel") == false
									|| ((NameSpace) el).getOwner()
											.isStereotyped("MongoDBModeler",
													"Database")) {
								return false;
							}
						}
						return true;
					}
				}, AuditSeverity.AuditWarning,
				"The package Database should be in package DataModel", "help");

		modelValidator.registerRule(new AbstractSimplifiedRule("MONGO_010c",
				org.modelio.metamodel.uml.statik.Package.class) {
			@Override
			public boolean check(MObject obj, List<Object> linked) {
				ModelElement el = (ModelElement) obj;
				if (el.isStereotyped("MongoDBModeler", "Database")) {

					return !el.getCompositionChildren().isEmpty();

				}

				return true;
			}
		}, AuditSeverity.AuditWarning, "The package Database is empty", "help");

		modelValidator.registerRule(new AbstractSimplifiedRule("MONGO_011a",
				org.modelio.metamodel.uml.statik.Package.class) {
			@Override
			public boolean check(MObject obj, List<Object> linked) {
				ModelElement el = (ModelElement) obj;
				if (el.isStereotyped("MongoDBModeler", "Collection")) {

					return el.isStereotyped("PersistentProfile", "DataModel");

				}

				return true;
			}
		}, AuditSeverity.AuditWarning,
				"The package Collection should be stereotyped DataModel",
				"help");

		modelValidator.registerRule(
				new AbstractSimplifiedRule("MONGO_011b",
						org.modelio.metamodel.uml.statik.Package.class) {
					@Override
					public boolean check(MObject obj, List<Object> linked) {
						ModelElement el = (ModelElement) obj;
						if (el.isStereotyped("MongoDBModeler", "Collection")) {
							if (((NameSpace) el).getOwner().isStereotyped(
									"MongoDBModeler", "Database") == false
									|| ((NameSpace) el).getOwner()
											.isStereotyped("MongoDBModeler",
													"Collection")) {
								return false;
							}
						}
						return true;
					}
				}, AuditSeverity.AuditWarning,
				"The package Collection should be in package Database", "help");

		modelValidator.registerRule(new AbstractSimplifiedRule("MONGO_011c",
				org.modelio.metamodel.uml.statik.Package.class) {
			@Override
			public boolean check(MObject obj, List<Object> linked) {
				ModelElement el = (ModelElement) obj;
				if (el.isStereotyped("MongoDBModeler", "Collection")) {

					return !el.getCompositionChildren().isEmpty();

				}

				return true;
			}
		}, AuditSeverity.AuditWarning, "The package Collection is empty",
				"help");

		modelValidator.registerRule(
				new AbstractSimplifiedRule("MONGO_011d",
						org.modelio.metamodel.uml.statik.Package.class) {
					@SuppressWarnings("unchecked")
					@Override
					public boolean check(MObject obj, List<Object> linked) {
						ModelElement el = (ModelElement) obj;
						if (el.isStereotyped("MongoDBModeler", "Collection")) {

							List<MObject> childs = (List<MObject>) el
									.getCompositionChildren();
							for (MObject child : childs) {
								if (child instanceof Package)
									return false;
							}
						}
						return true;
					}
				}, AuditSeverity.AuditWarning,
				"The Collection package should not contain package", "help");

		modelValidator.registerRule(
				new AbstractSimplifiedRule("MONGO_011e",
						org.modelio.metamodel.uml.statik.Package.class) {
					@SuppressWarnings("unchecked")
					@Override
					public boolean check(MObject obj, List<Object> linked) {
						ModelElement el = (ModelElement) obj;
						if (el.isStereotyped("MongoDBModeler", "Collection")) {

							List<MObject> childs = (List<MObject>) el
									.getCompositionChildren();
							int nbText = 0;
							for (MObject child : childs) {
								if (((ModelElement) child).isStereotyped(
										"MongoDBModeler", "Index")) {
									for (Dependency dependency : ((ModelElement) child)
											.getDependsOnDependency()) {
										if (dependency
												.getTagValue("MongoDBModeler",
														"description") != null) {
											if (dependency.getTagValue(
													"MongoDBModeler",
													"description").equals(
													"text")) {
												nbText++;

											}
										}
									}
									return nbText <= 1;
								}
							}

							return nbText <= 1;
						}
						return true;
					}
				}, AuditSeverity.AuditWarning,
				"Collection should have only most one \"text\" index", "help");

		modelValidator.registerRule(
				new AbstractSimplifiedRule("MONGO_012",
						org.modelio.metamodel.uml.statik.Class.class) {
					@Override
					public boolean check(MObject obj, List<Object> linked) {
						ModelElement el = (ModelElement) obj;
						if (el.isStereotyped("MongoDBModeler", "Document")) {

							return ((NameSpace) el).getOwner().isStereotyped(
									"MongoDBModeler", "Collection");

						}

						return true;
					}
				}, AuditSeverity.AuditWarning,
				"A Document should be in package Collection", "help");

		modelValidator.registerRule(
				new AbstractSimplifiedRule("MONGO_013",
						org.modelio.metamodel.uml.statik.Class.class) {
					@Override
					public boolean check(MObject obj, List<Object> linked) {
						ModelElement el = (ModelElement) obj;
						if (el.isStereotyped("MongoDBModeler", "Index")) {

							return ((NameSpace) el).getOwner().isStereotyped(
									"MongoDBModeler", "Collection");

						}

						return true;
					}
				}, AuditSeverity.AuditWarning,
				"An Index should be in package Collection", "help");

		modelValidator.registerRule(
				new AbstractSimplifiedRule("MONGO_014",
						org.modelio.metamodel.uml.statik.Class.class) {
					@Override
					public boolean check(MObject obj, List<Object> linked) {
						ModelElement el = (ModelElement) obj;
						if (el.isStereotyped("MongoDBModeler", "ShardingKey")) {

							return ((NameSpace) el).getOwner().isStereotyped(
									"MongoDBModeler", "Collection");

						}

						return true;
					}
				}, AuditSeverity.AuditWarning,
				"A Shardingkey should be in package Collection", "help");
	}

}
