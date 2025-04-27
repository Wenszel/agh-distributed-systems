const readline = require('readline');

function setupCommandListener(client) {
    const rl = readline.createInterface({
        input: process.stdin,
        output: process.stdout
    });

    rl.on('line', (input) => {
        const parts = input.trim().split(' ');

        if (parts[0] === 'u') {
            const cIndex = parts.indexOf('-c');
            const eIndex = parts.indexOf('-e');

            let cities = [];
            let eventTypes = [];

            if (cIndex !== -1 && parts[cIndex + 1]) {
                cities = parts[cIndex + 1].split(',');
            }
            if (eIndex !== -1 && parts[eIndex + 1]) {
                eventTypes = parts[eIndex + 1].split(',').map(Number);
            }

            console.log('Unsubscribing from:', cities, eventTypes);
            
            performUnsubscribe(client, cities, eventTypes);
        } else {
            console.log('Unknown command:', input);
        }
    });
}

function performUnsubscribe(client, cities, eventTypes) {
    const unsubscribeRequest = {
        cities: cities,
        event_types: eventTypes
    };

    client.unsubscribe(unsubscribeRequest, (error, response) => {
        if (error) {
            console.error('Error during unsubscribe:', error);
        } else {
            console.log('Unsubscribe success:', response.success);
        }
    });
}

module.exports = { setupCommandListener };