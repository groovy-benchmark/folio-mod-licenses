#!/bin/bash -e

# Add the deps.
DEPS='curl jq'
for DEP in $DEPS; do
  if [ ! -z "$(which $DEP)" ]; then
    echo "Found $DEP"
  else
    echo "Could not find $DEP, please ensure you have it installed." 1>&2
    exit 1
  fi
done

export TARGET="http://localhost:8081"

# echo get licenses config
# curl --header "X-Okapi-Tenant: diku" -H "Content-Type: application/json" -X GET http://localhost:8080/licenses/kiwt/config

echo "Load file 'license_properties.jq'"
# Because of the template placeholders in the file now, we load passing in empty object as substitutions.
json_data_file=`cat license_properties.jq`
json_data=`echo '{}' | jq "$json_data_file"`
json_result="$json_data"


IFS=$'\n'       # make newlines the only separator from this point onwards.
echo "Ensure refdata categories exist for the properties, and enrich the data."

categories=$(echo $json_data | jq -rc '.refdataCategories | to_entries[]')
# categoriesis now new-line separated list of values with {"key": key, "value": value} 
for cat in $categories; do
  entry_key=$(echo $cat | jq -rc '.key')
  entry_value=$(echo $cat | jq -rc '.value')
  entry_desc=$(echo $entry_value | jq -rc '.desc')
  
  result=$(curl -sSLG -H 'Accept:application/json' -H 'Content-Type: application/json' -H 'X-OKAPI-TENANT: diku' "$TARGET/licenses/refdata" \
    --data-urlencode "filters=desc==${entry_desc}")
    
  if [ "$( echo $result | jq 'length')" = 0 ]; then
    # We need to add the data.
    echo "Adding the refdata category ${entry_desc}."
    result=$(curl -sSLf -H 'Accept:application/json' -H 'Content-Type: application/json' -H 'X-OKAPI-TENANT: diku' -XPOST "$TARGET/licenses/refdata" -d "${entry_value}") 
  else
    echo "Refdata category ${entry_desc} already exists."
    # Grab the first value.
    result=$(echo $result | jq '.[0]')
  fi
  
  echo $result | jq

  # Write the value returned from the server to the json data internally for substitution.
  json_data=`echo "$json_data" | jq ".refdataCategories.${entry_key}=${result}"`
done

# Reload from the file, enriching with the added/looked up refdata objects.
json_result=`echo "$json_data" | jq "$json_data_file"`

echo "Load prop defs"
count=0
for row in $(echo "$json_result" | jq -rc ".propertyDefinitions[]" ); do
  echo "Posting ${row}"
  result=$(curl -sSL -H 'Accept:application/json' -H 'Content-Type: application/json' -H 'X-OKAPI-TENANT: diku' -XPOST "$TARGET/licenses/custprops" -d "${row}")
  echo $result | jq
  json_data=`echo "$json_data" | jq ".propertyDefinitions[$count] = $result"`
  count=$((count+1)) 
done

# Reload JSON result with any returned vals above.
json_result=`echo "$json_data" | jq "$json_data_file"`

# Load the licenses.
echo "Load licenses"
count=0
for row in $(echo "$json_result" | jq -rc ".licenseDefs[]"); do
  echo "Posting ${row}"
  result=$(curl -sSL -H 'Accept:application/json' -H 'Content-Type: application/json' -H 'X-OKAPI-TENANT: diku' -XPOST "$TARGET/licenses/licenses" -d "${row}")
  echo $result | jq
  json_data=`echo "$json_data" | jq ".licenseDefs[$count] = $result"`
  count=$((count+1))
done

echo "Final JSON data"
echo "$json_data" | jq

# echo Run a search for licenses
# ./okapi-cmd /licenses/licenses
# ./okapi-cmd /licenses/refdata/License/type
# ./okapi-cmd /licenses/org
# ./okapi-cmd /licenses/refdata/LicenseOrg/role

