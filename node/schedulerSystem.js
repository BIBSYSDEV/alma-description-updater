
const AWS = require("aws-sdk");
AWS.config.update({region: 'eu-west-1'});
const sqs = new AWS.SQS({ apiVersion: "2012-11-05" });

exports.almaWriter = function (event, context) {
    console.log('Received records:'+ event.Records.length);
    console.log('Received records:'+ JSON.stringify(event));
    
    event.Records.forEach(function(record) {
        console.log("Message: "+record.messageId);
    });    
}

exports.streamReader =  function(event, context) {
    console.log('Received records:'+ event.Records.length);
    event.Records.forEach(function(record) {
        
        // CHECK EACH RECORD AND TRANSFORM/SPLIT
        var params = {MessageBody: JSON.stringify(record), QueueUrl: process.env.SqsUrlAlma };

        sqs.sendMessage(params).promise()
        .then(data => console.log("Successfully added message to queue", data.MessageId))
        .catch(err => console.log("There was an Error: ", err));
    });

}

exports.almaErrorHandler =  function(event, context) {
    console.info('Received records:'+ event.Records.length);
    event.Records.forEach(function(record) {
        console.log("Bok: "+JSON.parse(record.body).dynamodb.Keys.isbn.S); //FOR DEMO
        if (JSON.parse(record.body).dynamodb.Keys.isbn.S == 5)  // FOR DEMO - ERSTATT MED COUNTER>n eller sjekk timestamp fra DynamoDB for å unngå evig loop
            throw new Error("Error bok 5");
    });
}

exports.almaErrorScheduler =  function(event, context) {
    console.log(JSON.stringify(event));
    console.info('Received records:'+ event.Records.length);
    event.Records.forEach(function(record) {
        var alarm = JSON.parse(record.Sns.Message);
        console.info("Alarm: "+alarm.NewStateValue+" "+alarm.NewStateReason);
        alarm.NewStateValue=="ALARM" ? EnableEventSource=true : EnableEventSource=false;
    });

    lambda.updateEventSourceMapping({UUID:process.env.ErrorHandlerSwitch, Enabled:EnableEventSource}).promise()
        .then(data => console.info(console.info("SQS event source:", data.State)))
        .catch(err => console.error("There was an Error: ", err));
}




