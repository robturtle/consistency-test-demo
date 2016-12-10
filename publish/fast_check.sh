./client.sh -server localhost:9091 -set a 1
sleep 1

for i in {1..8}; do
    ./client.sh -server localhost:9091 -set a 1$i && echo w 1$i
    echo r $(./client.sh -server localhost:9091 -get a)
done
