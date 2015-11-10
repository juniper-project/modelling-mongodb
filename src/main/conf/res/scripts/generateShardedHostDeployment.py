#
# Copyright 2014 Modeliosoft
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#
from modelling import hasStereotype

program = selectedElements.get(0)
model = program.getOwner().getOwner()
hwPlat = [p for p in model.getOwnedElement(Package) if hasStereotype(p, 'HardwarePlatformModel')][0]

def create_node(hwPlat):
	node = modelingSession.getModel().createInstance('node', hwPlat)
	node.addStereotype('MARTEDesigner', 'HwComputingResource_Instance')
	node.addStereotype('JuniperIDE', 'CloudNode')
	
	cpu = modelingSession.getModel().createBindableInstance()
	cpu.setName('CPU')
	node.getPart().add(cpu)
	cpu.addStereotype('MARTEDesigner', 'HwProcessor_Instance')
	cpu.addStereotype('JuniperIDE', 'CloudNodeCPU')
	return node

def create_program_bindableInstance(hwPlat, node, program):
	pInst = modelingSession.getModel().createBindableInstance()
	pInst.setName(program.getName())
	node.getPart().add(pInst)
	#pInst.addStereotype('JuniperIDE', 'ProgramInstance')
	pInst.setBase(program)
	return pInst
	
nodeR = create_node(hwPlat)
nodeR.setName(program.getName()+"_router")
router = create_program_bindableInstance(hwPlat, nodeR, program)
router.setName('router')
router.addStereotype('MongoDBModeler', 'Router')
router.addStereotype('JuniperIDE', 'ProgramInstance')

nodeS  = create_node(hwPlat)
nodeS.setName(program.getName()+"_shard")
shard = create_program_bindableInstance(hwPlat, nodeS, program)
shard.setName('shard')
shard.addStereotype('MongoDBModeler', 'Shard')
shard.addStereotype('JuniperIDE', 'ProgramInstance')

nodeC  = create_node(hwPlat)
nodeC.setName(program.getName()+"_config")
config = create_program_bindableInstance(hwPlat, nodeC, program)
config.setName('config')
config.addStereotype('MongoDBModeler', 'ConfigServer')
config.addStereotype('JuniperIDE', 'ProgramInstance')