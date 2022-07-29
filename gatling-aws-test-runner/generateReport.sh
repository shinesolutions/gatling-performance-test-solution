#!/bin/bash
set -e

function help_text {
    cat <<EOF
    Usage: $0 [ -c|--clear-logs CLEAR_LOGS ] [ -u|--upload-report UPLOAD_REPORT ] [ -r|--report-bucket REPORT_BUCKET ] [ -p|--profile AWS_PROFILE ] [-h]

        --clear-logs                            (optional) Clear the log folder in the S3 bucket after creating the report.
        --upload-report                         (optional) Upload HTML report to S3 bucket.
        --report-bucket REPORT_BUCKET           (required) name of the S3 bucket to download logs from and upload the reports to.
        --profile AWS_PROFILE                   (optional) The profile to use from ~/.aws/credentials.
EOF
    exit 1
}

while [ $# -gt 0 ]; do
    arg=$1
    case $arg in
        -h|--help)
            help_text
        ;;
        -r|--report-bucket)
            REPORT_BUCKET="$2"
            shift; shift
        ;;
        -c|--clear-logs)
            CLEAR_LOGS=true
            shift; shift;
        ;;
        -u|--upload-report)
            UPLOAD_REPORT=true
            shift; shift;
        ;;
        -p|--profile)
            export AWS_DEFAULT_PROFILE="$2"
            shift; shift
        ;;
        *)
            echo "ERROR: Unrecognised option: ${arg}"
            help_text
            exit 1
        ;;
    esac
done

if [[ -z $REPORT_BUCKET ]]
then
    echo "Report bucket required."
    help_text
    exit 1
fi

RUN_ID="Report"-${SIMULATION}-${SIMULATION_TYPE}-${ENVIRONMENT}-$(date +%Y%m%d-%H%M%S)


echo '******* PARAMETERS *******'
echo 'REPORT_BUCKET: ' ${REPORT_BUCKET}
echo 'CLEAR_LOGS: ' ${CLEAR_LOGS}
echo 'UPLOAD_REPORT: ' ${UPLOAD_REPORT}
echo 'AWS_DEFAULT_PROFILE: ' ${AWS_DEFAULT_PROFILE}
echo 'RUN_ID: ' ${RUN_ID}

# Determine script dir and move to it
DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )
echo 'DIR: ' ${DIR}/..
#cd ${DIR}/..

# Create a directory for results
mkdir -p target/gatling/${RUN_ID}
echo 'Directory created: ' ${RUN_ID}
RESULTS_DIRECTORY=target/gatling/${RUN_ID}/ #TODO: Make RUN_ID an env variable to be provided by runner
echo 'RESULTS_DIRECTORY: ' ${RESULTS_DIRECTORY}

# Safeguard only - remove any files in the results directory if exist
rm -f -r ${RESULTS_DIRECTORY}

## Download all logs for all test gatling clients
aws s3 cp s3://${REPORT_BUCKET}/logs/ ${RESULTS_DIRECTORY} --recursive --no-progress

## Consolidate the reports
mvn gatling:test -Dgatling.reportsOnly=${RUN_ID}

#Go to current results directory i.e. target/gatling
cd ${RESULTS_DIRECTORY}/..

#Set the tar (compressed) file name
TAR_FILENAME=${RUN_ID}.tar.gz
echo 'TAR_FILENAME: ' ${TAR_FILENAME}

#Create the tar file
tar -zcf ${TAR_FILENAME} -C ${RUN_ID} .

if [ "${CLEAR_LOGS}" = true ]
then
  ## Delete everything in the logs subdirectory of $REPORT_BUCKET in S3
  aws s3 rm s3://${REPORT_BUCKET}/logs --recursive
  echo "Deleted simulation logs."
fi

if [ "${UPLOAD_REPORT}" = true ]
then
  ## Upload tar file to S3
  aws s3 cp ${TAR_FILENAME} s3://${REPORT_BUCKET}/reports/
  echo "Uploaded results to S3 bucket: s3://${REPORT_BUCKET}/reports"
fi
