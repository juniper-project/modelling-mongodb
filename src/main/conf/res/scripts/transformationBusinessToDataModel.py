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

def createTraceabilityLink(el, target):
 	modelingSession.getModel().createDependency(el, target, "ModelerModule", "trace")
 				
def isCreated(package,el):
	for database in package.getCompositionChildren() :
		for collection in database.getCompositionChildren():
			for element in collection.getCompositionChildren():
				dependencies = element.getDependsOnDependency()
				print dependencies
				if len(dependencies)>0 :
					for dependency in dependencies :
						if dependency.isStereotyped('ModelerModule','trace') :
							print dependency.getDependsOn() == el
							if dependency.getDependsOn() == el :						
								return dependency.getImpacted()
	return None
 				
business = selectedElements.get(0)
software = business.getOwner()
datamodel = modelingSession.getModel().createPackage('Business datamodel', software,'PersistentProfile','DataModel')
database = modelingSession.getModel().createPackage('Business database', datamodel,'MongoDBModeler','Database')
database.addStereotype('PersistentProfile','DataModel')
collection = modelingSession.getModel().createPackage('business collection', database,'MongoDBModeler','Collection')
collection.addStereotype('PersistentProfile','DataModel')

for child in business.getCompositionChildren() : 
	if child.isStereotyped('PersistentProfile','Entity'):
		document = isCreated(datamodel,child)
		if  document == None :
			document = modelingSession.getModel().createClass(child.getName(), collection,  'MongoDBModeler', 'Document')
			document.addStereotype('PersistentProfile','Entity')
			createTraceabilityLink(document,child)
		for attribut in  child.getOwnedAttribute() :
			attribute = modelingSession.getModel().createAttribute(attribut.getName(),attribut.getType(),document)
			attribute.setMultiplicityMin(attribut.getMultiplicityMin())
			attribute.setMultiplicityMax(attribut.getMultiplicityMax())
			createTraceabilityLink(attribute, attribut)
			if attribut.isStereotyped('JuniperIDE','HorizontalPartitioning'):
				shardingKey = modelingSession.getModel().createClass('ShardingKey', collection,  'MongoDBModeler', 'ShardingKey')
				createTraceabilityLink(shardingKey, attribut)
				dependency = modelingSession.getModel().createDependency(shardingKey, attribute, 'MongoDBModeler','Sharding')
				createTraceabilityLink(dependency, attribut)
			#for dependency in attribut.getImpactedDependency() : 
			#if dependency.getImpacted().isStereotyped('JuniperIDE','vertical partitionning') :
				
		for associationEnd in child.getOwnedEnd() :
			print associationEnd.getOppositeOwner().getOwner()
			doc = isCreated(datamodel,associationEnd.getOppositeOwner().getOwner())
			print doc
			if  doc != None :
				aggregation= associationEnd.getAggregation()
				endName=associationEnd.getName()
				multiplicityMin = associationEnd.getMultiplicityMin()
				multiplicityMax = associationEnd.getMultiplicityMax()
				association = modelingSession.getModel().createAssociation(document,doc,doc.getName())
				association.getEnd().get(1).setAggregation(aggregation)
				association.getEnd().get(1).setName(endName)
				association.getEnd().get(1).setMultiplicityMin(multiplicityMin)
				association.getEnd().get(1).setMultiplicityMax(multiplicityMax)
				
			else :
				doc = modelingSession.getModel().createClass(associationEnd.getOppositeOwner().getOwner().getName(), collection,  'MongoDBModeler', 'Document')
				doc.addStereotype('PersistentProfile','Entity')
				createTraceabilityLink(doc,associationEnd.getOppositeOwner().getOwner())
				aggregation = associationEnd.getAggregation()
				endName = associationEnd.getName()
				multiplicityMin = associationEnd.getMultiplicityMin()
				multiplicityMax = associationEnd.getMultiplicityMax()
				association = modelingSession.getModel().createAssociation(document,doc,doc.getName())
				association.getEnd().get(1).setAggregation(aggregation)
				association.getEnd().get(1).setName(endName)
				association.getEnd().get(1).setMultiplicityMin(multiplicityMin)
				association.getEnd().get(1).setMultiplicityMax(multiplicityMax)
			