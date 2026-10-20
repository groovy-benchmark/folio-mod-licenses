package org.olf.licenses;

import grails.testing.mixin.integration.Integration
import spock.lang.Shared
import spock.lang.Stepwise
import spock.lang.Unroll

@Stepwise
@Integration
class LookupSpec extends BaseSpec {
  
  @Shared
  Map<String, String> data = [
    'refdata' : [:],
    'licenses' : [:],
    'license_properties' : [:]
  ]

  @Unroll
  void 'Add #description Refdata' (String description, List vals) {
    given: 'Data posted'
    Map httpResult = doPost('/licenses/refdata') {
      desc description
      values (vals) {
        label "${it}"
      }
    }
    data['refdata'][description] = httpResult.id

    expect:
    assert httpResult.id != null
    assert httpResult.get('values')?.size() == vals.size()

    where:
    [description, vals] << [
      ['Yes/No/Other', ['Yes', 'No', 'Other']],
      ['Permitted/Prohibited', [
        'Permitted (explicit)',
        'Permitted (explicit) under conditions',
        'Permitted (interpreted)',
        'Prohibited (explicit)',
        'Prohibited (interpreted)',
        'Unmentioned',
        'Not applicable'
      ]]
    ]
  }

  void 'Add License Properties' (Closure payload) {
    given: 'Property posted'
    Map httpResult = doPost('/licenses/custprops', payload)

    and:
    data['license_properties'][httpResult.name] = httpResult.id

    expect:
    assert httpResult.id != null

    where:
    payload << [{
                  name "concurrentAccess"
                  type "Integer"
                  label "Number of concurrent users allowed"
                  description "The number of concurrent users allowed by the resource"
                  primary true
                },{
                  name "authorisedUsers"
                  type "Text"
                  label "Definition of authorised user"
                  description "The definition of an authorised user for a resource"
                  weight (-1)
                  primary true
                  defaultInternal false
                },{
                  name "remoteAccess"
                  category data['refdata']['Yes/No/Other']
                  type "Refdata"
                  label "Access restricted to on-campus/campus network?"
                  description "Can access to the resource be provided from outside the library or institutional location / network"
                  primary true
                  defaultInternal false
                },{
                  name "illElectronic"
                  category data['refdata']['Permitted/Prohibited']
                  type "Refdata"
                  label "Electronic ILL"
                  description "The right to provide the licensed materials via interlibrary loan by way of electronic copies"
                  primary true
                }]
  }

  void 'Add Licenses' (payload, Map termData) {

    given: 'Create new License'
    Map httpResult = doPost('/licenses/licenses', payload)

    and:
    data['licenses'][httpResult.name] = httpResult

    expect: 'License created'
    httpResult.id != null

    and: 'Correct term data'
    termData.every { Map.Entry it ->
      final prop = httpResult.customProperties[it.key][0]
      return prop.internal == it.value.internal &&
        (it.value.note == null || prop.note == it.value.note) &&
        (it.value.publicNote == null || prop.publicNote == it.value.publicNote)
    }

    where:

    payload << [{
                  name "License 1"
                  description "License for the AAAS Science Classic Agreement"
                  status "Active"
                  type "Local"
                  startDate "2019-01-01"
                  openEnded true
                  customProperties {
                    concurrentAccess ([[value:10, note: 'Note for concurrentAccess', publicNote: 'Public note for concurrentAccess']])
                    authorisedUsers ([[value:"Anyone with campus login", note: 'Note for authorisedUsers', publicNote: 'Public note for authorisedUsers']])
                    remoteAccess ([[value:"Yes"]])
                    illElectronic ([[value:"Prohibited (interpreted)"]])
                  }
                  docs ([
                    {
                      name "A test document attachment"
                      location "http://a.b.c/d/e/f.doc"
                      url "http://a.b.c/d"
                      note "This is a document attachment"
                      atType "License"
                    },
                    {
                      name "A test document attachment"
                      location "http://a.b.c/d/e/g.doc"
                      url "http://a.b.c/e"
                      note "This is a second document attachment"
                      atType "Misc"
                    }
                  ])
                  supplementaryDocs ([
                    {
                      name "A test SD document attachment 1"
                      location "http://a.b.c/d/e/f.doc"
                      note "This is a document attachment"
                      atType "License"
                    },
                    {
                      name "A test SD document attachment 2"
                      location "http://a.b.c/d/e/g.doc"
                      note "This is a second document attachment"
                      atType "Misc"
                    }
                  ])
                },
                {
                  name "Test License 2"
                  status "Active"
                  type "Local"
                  description "This is a test licenses"
                  startDate "2019-01-01"
                  endDate "2020-01-01"
                  endDateSemantics "Explicit"
                  customProperties {
                    concurrentAccess ([[value:20, internal: false]])
                    authorisedUsers ([[value:"Open access", internal: true]])
                    remoteAccess ([[value:"No", internal: true]])
                    illElectronic ([[value:"Unmentioned", internal: false]])
                  }
                  tags ([
                    "legacy"
                  ])
                  orgs ([
                    {
                      role "Licensor"
                      org {
                        name "EBSCO"
                        orgsUuid "1234-5678-9102-3356"
                      }
                    },
                    {
                      role "Licensee"
                      org {
                        name "Some Content Provider"
                        orgsUuid "1234-5678-9102-3355"
                      }
                    }
                  ])
                }]

    // First set should use defaults, second should be overridden
    termData << [[
                   'concurrentAccess': [internal: true, note: 'Note for concurrentAccess', publicNote: 'Public note for concurrentAccess'],
                   'authorisedUsers': [internal: false, note: 'Note for authorisedUsers', publicNote: 'Public note for authorisedUsers'],
                   'remoteAccess': [internal: false],
                   'illElectronic': [internal: true]
                 ],[
                   'concurrentAccess': [internal: false],
                   'authorisedUsers': [internal: true],
                   'remoteAccess': [internal: true],
                   'illElectronic': [internal: false]
                 ]]
  }
  
  void 'Check we have licenses' () {
    given: 'Lookup licence'
      Map httpResult = doGet('/licenses/licenses', [
        stats: true, // Returns object with metadata instead of list
      ])
    expect:
      (data.total = httpResult.totalRecords) > 0
  }
  
  @Unroll
  void 'Lookup by #propertyName = #value' (final String propertyName, final def value, final int expect) {
    
    given: 'Lookup licence'
      Map httpResult = doGet('/licenses/licenses', [
        stats: true, // Returns object with metadata instead of list
        filters: [
          "customProperties.${propertyName}==${value}"
        ]
      ])
    expect:
      httpResult.totalRecords == expect
      
    where:
      propertyName                    | value       || expect
      'remoteAccess.value.value'      | 'yes'       || 1
      'concurrentAccess.value'        | 20          || 1
      'remoteAccess.value.value'      | 'no'        || 1
      'concurrentAccess.value'        | 32          || 0
      'remoteAccess'                  | 'no'        || data.total
      'concurrentAccess'              | 32          || data.total
  }
  
  
  
  @Unroll
  void 'Lookup refdata by id #propertyName = #value from category #rdc' (final String propertyName, final String rdc, final def value, final int expect) {
    
    when: 'Lookup rdv id from category_id=#rdc and value #value'
      def cat = doGet("/licenses/refdata/${rdc}")
      
      def val = cat.values.find { it.value == value }
      
    then: 'Value found'
      val?.id != null
      
    when: 'Lookup licence'
      Map httpResult = doGet('/licenses/licenses', [
        stats: true, // Returns object with metadata instead of list
        filters: [
          "customProperties.${propertyName}==${val.id}"
        ]
      ])
    then:
      httpResult.totalRecords == expect
      
    where:
      propertyName              | rdc                               | value       || expect
      'remoteAccess.value'      | data['refdata']['Yes/No/Other']   | 'yes'       || 1
      'remoteAccess.value'      | data['refdata']['Yes/No/Other']   | 'no'        || 1
  }
}