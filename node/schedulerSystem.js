
const AWS = require("aws-sdk");
AWS.config.update({region: 'eu-west-1'});
const sqs = new AWS.SQS({ apiVersion: "2012-11-05" });
const lambda = new AWS.Lambda();

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
        let timestamp = JSON.parse(record.body).dynamodb.ApproximateCreationDateTime;
        let msgCreationDate = new Date(timestamp * 1000);
        console.log("date message was created: "+ msgCreationDate);
        let sixMonthAgo = addMonths(Date.now(), process.env.MessageAgeLimitAlmaUpdateQueue);
        let queueUrl;
        if (msgCreationDate < sixMonthAgo)
        {
            queueUrl = process.env.SqsUrlAlmaDLQ;
        } else {
            queueUrl = process.env.SqsUrlAlmaQ;
        }
        let params = {MessageBody: JSON.stringify(record), QueueUrl: queueUrl};
        sqs.sendMessage(params).promise()
            .then(data => console.log("Successfully added message to queue", data.MessageId))
            .catch(err => console.log("There was an Error: ", err));

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


const addMonths = (input, months) => {
    const date = new Date(input)
    date.setDate(1)
    date.setMonth(date.getMonth() + months)
    date.setDate(Math.min(input.getDate(), getDaysInMonth(date.getFullYear(), date.getMonth()+1)))
    return date
}


const getDaysInMonth = (year, month) => new Date(year, month, 0).getDate()
