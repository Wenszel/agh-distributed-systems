const grpc = require('@grpc/grpc-js');
const protoLoader = require('@grpc/proto-loader');
const { setupCommandListener } = require('./command');

const PROTO_PATH = './subscription.proto';

const packageDefinition = protoLoader.loadSync(PROTO_PATH);
const subscriptionProto = grpc.loadPackageDefinition(packageDefinition).subscription;

let client = new subscriptionProto.EventService('localhost:50051', grpc.credentials.createInsecure());

let retryAttempts = 0;  
const MAX_RETRIES = 5;

const request = {
    cities: ['Warszawa'],
    eventTypes: [2]
};

function performSubscribe(client, request) {
    const call = client.subscribe(request);
    
    call.on('data', notification => {
        retryAttempts = 0;  
        console.log('Received event:', notification);
    });

    call.on('end', () => {
        console.log('Server ended call');
    });

    call.on('error', err => {
        if (err.code === grpc.status.UNAVAILABLE) {
            console.log("Connection lost, attempting to reconnect...");
            if (retryAttempts < MAX_RETRIES) {
                retryAttempts++;
                setTimeout(() => {
                    console.log(`Retrying connection... (${retryAttempts}/${MAX_RETRIES})`);
                    client = new subscriptionProto.EventService('localhost:50051', grpc.credentials.createInsecure());
                    performSubscribe(client, request);
                }, 5000);  
            } else {
                console.log("Max retries reached, could not reconnect.");
            }
        } else {
            console.error('Error:', err);
        }
    });
}

performSubscribe(client, request);
setupCommandListener(client);