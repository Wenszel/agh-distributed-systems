const grpc = require('@grpc/grpc-js');
const protoLoader = require('@grpc/proto-loader');
const { setupCommandListener } = require('./command');

const PROTO_PATH = './subscription.proto';

const packageDefinition = protoLoader.loadSync(PROTO_PATH);
const subscriptionProto = grpc.loadPackageDefinition(packageDefinition).subscription;

const client = new subscriptionProto.EventService('localhost:50051', grpc.credentials.createInsecure());

const request = {
    cities: ['Warsaw'],
    eventTypes: [subscriptionProto.EventType.type.value.number]
};

const call = client.subscribe(request);

call.on('data', notification => {
    console.log('Received event:', notification);
});

call.on('end', () => {
    console.log('Server ended call');
});

call.on('error', err => {
    console.error('Error:', err);
});

call.on('status', status => {
    console.log('Status:', status);
});

setupCommandListener(client);