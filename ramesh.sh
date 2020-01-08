i=`kubectl get deployments|awk '{print $1}'`
n=tomcat-deployment
for name in $i;
do
echo $name
if [ "$name"=="$n" ];
then
   kubectl delete deployment tomcat-deploymet
else
        echo "no issues"
fi
done
