set -x

while read BINARY FILE
do
  case $BINARY in
    "dir" )
        : ;;
    "text" )
      if tr -d '\015' < /dev/$FILE > /dev/$FILE.new ;
      then : ; else exit 1 ; fi ;;
      if mv -f /dev/$FILE.new /dev/$FILE ;
      then : ; else exit 1 ; fi ;;
    "binary" )
      : ;;
  esac ;
done

  