
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


