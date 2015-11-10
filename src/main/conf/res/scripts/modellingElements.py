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



    
juniper = selectedElements.get(0)
childs = [child for child in juniper.getCompositionChildren() if child.isStereotyped('JuniperIDE', 'SoftwareArchitectureModel')] 
for childSoft in childs :
	servers = [server for server in childSoft.getCompositionChildren() if server.isStereotyped('MongoDBModeler', 'MongoDBServer')]
	if len(servers) != 0 :
		interface = modelingSession.getModel().createInterface('IRelationalDatabase', childSoft)

for server in servers :
	
	getIp = modelingSession.getModel().createOperation('getIp', server)	
	attributeIp=modelingSession.getModel().createAttribute('ip', modelingSession.getModel().getUmlTypes().getSTRING(), server)
	hosts = server.getRepresenting()
	if len(hosts) >1 :
		for host in hosts :
			if host.isStereotyped('MongoDBModeler','Router') or host.isStereotyped('MongoDBModeler','Primary') :
				ip=host.getCluster().getTagValue('JuniperIDE','ip')
				attributeIp.setValue(ip)
	else :
		ip=hosts.get(0).getCluster().getTagValue('JuniperIDE','ip')
	 	attributeIp.setValue(ip)
	 	
	modelingSession.getModel().createReturnParameter('ip', modelingSession.getModel().getUmlTypes().getSTRING(), getIp)	
	modelingSession.getModel().createInterfaceRealization(server, interface)
	
	

	
