set -x

IC_BUILD_DIR=/usr/gds.linux5/postler/dev/build
export IC_BUILD_DIR

IC_DEV_DIR=/usr/gds.linux5/postler/dev
export IC_DEV_DIR

IC_PLATFORM=linux
export IC_PLATFORM

echo "buildOnLinux5.sh should only be run from IC_DEV_DIR, never IC_BUILD_DIR."

echo "The file build.sh being used is from IC_DEV_DIR."
if $IC_DEV_DIR/sbin/build.sh $1 $2 $3 $4
then 
     echo "InterClient Build SUCCEEDED"
else 
     echo "InterClient Build FAILED"
fi

