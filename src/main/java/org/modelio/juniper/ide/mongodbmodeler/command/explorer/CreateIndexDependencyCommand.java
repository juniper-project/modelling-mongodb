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

import java.util.List;

import org.modelio.api.module.IModule;
import org.modelio.metamodel.uml.infrastructure.ModelElement;
import org.modelio.metamodel.uml.statik.Attribute;
import org.modelio.vcore.smkernel.mapi.MObject;

public class CreateIndexDependencyCommand extends RunScriptCommand {

	public CreateIndexDependencyCommand() {
		super("createIndexDependency");
	}

	@Override
	public boolean accept(List<MObject> selectedElements, IModule module) {
		boolean containsIndex = false;
		boolean containsAttribute = false;
		for (MObject element : selectedElements) {
			if (((ModelElement) element).isStereotyped("MongoDBModeler",
					"Index")) {
				containsIndex = true;
				if (containsAttribute)
					break;

			} else if (((ModelElement) element) instanceof Attribute) {
				containsAttribute = true;
				if (containsIndex)
					break;
			}
		}
		return selectedElements.size() >= 2 && containsAttribute
				&& containsIndex;
	}
}
