#!/usr/bin/env bash

if [[ -z $1 ]]; then
  echo "USAGE: ${0} DIRECTORY"
  exit 1
fi

SCAN_PATH=${1}
FOOTPRINT_FOLDER=".footprint"
FOOTPRINT_LIST="${SCAN_PATH}/${FOOTPRINT_FOLDER}/list"


#alias exiftool=/share/CACHEDEV1_DATA/.qpkg/Entware/bin/exiftool
EXIFTOOL="/share/CACHEDEV1_DATA/.qpkg/Entware/bin/exiftool"

if [[ ! -d "${SCAN_PATH}/${FOOTPRINT_FOLDER}" ]]; then
  mkdir -p "${SCAN_PATH}/${FOOTPRINT_FOLDER}"
fi

hashGet() {
    local array=${1} index=${2}
    local i="${array}_$index"
    printf '%s' "${!i}"
}

scanStats() {
 for path in "${1}"*; do
    if [ -d "${path}" ];then
        scanStats "${path}/"
    elif [ -f "${path}" ]; then
      START=$(date +%s)
      STAT=$(stat -c '%s|%W|%Y' "${path}")
      LENGTH=$(echo "${STAT}" | cut -d'|' -f1)
      UPDATED_AT=$(echo "${STAT}" | cut -d'|' -f3)
      KEY=$(echo "${path}" | sed -E 's/[^a-zA-Z0-9]/_/g')
      #echo "cache [${KEY}]: $(hashGet list ${KEY})"

      SAME=0
      FOUND=0
      MESSAGE="${path}"
      if [[ -f ${FOOTPRINT_LIST} ]]; then
        #PREVIOUS=$(cat -e "${FOOTPRINT_LIST}" 2> /dev/null | grep "${path}|")
        PREVIOUS=$(hashGet list "${KEY}")
        if [[ -n ${PREVIOUS} ]]; then
          FOUND=1
          LAST_LENGTH=$(echo "${PREVIOUS}" | cut -d'|' -f2)
          LAST_UPDATED_AT=$(echo "${PREVIOUS}" | cut -d'|' -f4)

          if [[ "${LENGTH}" = "${LAST_LENGTH}" && ${UPDATED_AT} = "${LAST_UPDATED_AT}" ]]; then
            SAME=1
          fi
        fi
      fi
      if [[ ${SAME} = 0 ]]; then
        CREATED_AT=$(echo "${STAT}" | cut -d'|' -f2)
        if [[ ${CREATED_AT} = 'W' ]]; then
          CREATED_AT=${UPDATED_AT}
        fi
        CHECKSUM=$(sha256sum -b "${path}" | cut -d" " -f1)
        MESSAGE="${MESSAGE} (${LENGTH} bytes @ ${UPDATED_AT}) = ${CHECKSUM}"
        LINE="${path}|${LENGTH}|${CREATED_AT}|${UPDATED_AT}|${CHECKSUM}"

        if [[ ${FOUND} = 1 ]]; then
          grep -v "declare \"list_${KEY}=" "${FOOTPRINT_FOLDER}/cache" > "${FOOTPRINT_FOLDER}/.cache"
          mv "${FOOTPRINT_FOLDER}/.cache" "${FOOTPRINT_FOLDER}/cache"
          grep -v "${path}\|" "${FOOTPRINT_LIST}" > "${FOOTPRINT_LIST}.tmp"
          mv "${FOOTPRINT_LIST}.tmp" "${FOOTPRINT_LIST}"
        fi
        echo "declare \"list_${KEY}=${LINE}\"" >> "${FOOTPRINT_FOLDER}/cache"
        echo "${LINE}" >> "${FOOTPRINT_LIST}"
      else
        MESSAGE="${MESSAGE} did not change"
      fi

      mkdir -p "${FOOTPRINT_FOLDER}/metadata"
      METADATA_FILE="${FOOTPRINT_FOLDER}/metadata/${path}"
      METADATA_FILENAME=$(basename "${METADATA_FILE}")
      METADATA_FILENAME="${METADATA_FILENAME%.*}"
      METADATA_FILE="$(dirname "${METADATA_FILE}")/${METADATA_FILENAME}.txt"
      if [[ ! -f ${METADATA_FILE} || ${SAME} = 0 ]]; then
        mkdir -p "$(dirname "${METADATA_FILE}")"
        ${EXIFTOOL} "${path}" -D -G0:1:2:3:4 -f -q -t -ee -U > "${METADATA_FILE}"
        MESSAGE="${MESSAGE} [metadata extracted]"
      fi

      mkdir -p "${FOOTPRINT_FOLDER}/thumbnails"
      THUMBNAIL_FILE="${FOOTPRINT_FOLDER}/thumbnails/${path}"
      if [[ ! -f ${THUMBNAIL_FILE} || ${SAME} != 1 ]]; then
        mkdir -p "$(dirname "${THUMBNAIL_FILE}")"
        ffmpeg -i "${path}" -vframes 1 -an -vf "scale=500:-1" -y "${FOOTPRINT_FOLDER}/thumbnails/${path}" &> /dev/null
        MESSAGE="${MESSAGE} [thumbnail generated]"
      fi

      if [[ ${SAME} = 0 ]]; then
        echo -e "\033[2K\r[CHANGED] ${MESSAGE} in $(($(date +%s) - START))s"
      else
        echo -ne "\033[2K\r${MESSAGE} in $(($(date +%s) - START))s"
      fi
    fi
 done
 #${EXIFTOOL} . -r -D -G0:1:2:3:4 -f -ee --ext THM --ext LRV --SRT -U -w "${FOOTPRINT_FOLDER}/metadata/%d%f.txt"
}

PWD=$(pwd)
cd "${SCAN_PATH}" || exit 1

CACHE_ROTTEN=1
if [[ -f "${FOOTPRINT_FOLDER}/cache" ]]; then
  AGE=$(($(date +%s) - $(stat -c '%Y' "${FOOTPRINT_FOLDER}/cache")))
  if [[ ${AGE} -lt 86400 ]]; then
    CACHE_ROTTEN=0
  fi
fi

if [[ ! -f "${FOOTPRINT_FOLDER}/cache" || ${CACHE_ROTTEN} == 1 ]]; then
  #echo "Building cache"
  rm -f "${FOOTPRINT_FOLDER}/cache"
  I=0
  COUNT="$(wc -l "${FOOTPRINT_LIST}" | cut -d" " -f1)"
  while IFS= read -r line
  do
    key=$(echo "${line}" | cut -d"|" -f1 | sed -E 's/[^a-zA-Z0-9]/_/g')
    I=$((I + 1))
    echo -ne "\033[2K\rBuilding cache ${I}/${COUNT} ($((I * 100 / COUNT))%)"
    echo "declare \"list_${key}=${line}\"" >> "${FOOTPRINT_FOLDER}/cache"
  done < "${FOOTPRINT_LIST}"
  echo "\033[2K\rCache build"
fi

echo -n "Loading cache..."
. "${FOOTPRINT_FOLDER}/cache"
echo -e "\033[2K\rCache loaded    "

scanStats ""
echo -e "\033[2K\rScanning finished"

echo -n "Creating archives..."
tar -czvf .footprint/thumbnails.tar.gz -C .footprint/thumbnails .
tar -czvf .footprint/metadata.tar.gz -C .footprint/metadata .
echo -e "\033[2K\rArchives created"

cd "${PWD}" || exit 1
echo "Finished"
