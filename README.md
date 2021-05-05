# alma-description-updater
System that updates the description for posts in alma.

## Components:
* Lambdas
    * StreamReader
    * AlmaErrorHandler
    * AlmaErrorScheduler
    * AlmaUpdater
* Queues
    * AlmaUpdateQueue
    * AlmaUpdateDLQ
    * DynamoDbStreamDLQ
    * AlmaUpdateExhausted
* Alarms
    * UpdateSchedulerAlarm

## Lambdas:

### StreamReader
Location: node/schedulerSystem.js  
Entry point: DynamoDbStream  
Exit point 1(success): AlmaUpdateQueue  
Exit point 2(failure): DynamoDbStreamDLQ  
Purpose: Moving records from the dynamoDbStream into the AlmaUpdateQueue. 
The records are not altered along the way. 
In case of errors the records are written to the DynamoDbStreamDLQ.

### AlmaErrorHandler
Location: node/schedulerSystem.js  
Entry point: AlmaUpdateDLQ  
Exit point 1: AlmaUpdateQueue  
Exit point 2: AlmaUpdateExhausted  
Purpose: Moving records from the AlmaUpdateDLQ into the AlmaUpdateQueue. 
The records are not altered along the way. The lambda checks a timestamp on each record, 
if the record is older then a set timer the record is sent to the AlmaUpdateExhausted queue.

### AlmaErrorScheduler
Location: node/schedulerSystem.js  
Trigger: UpdateSchedulerAlarm  
Action: Enable/disable AlmaErrorHandler  
Purpose: Receives message from UpdateSchedulerAlarm telling it whether to 
enable or disable the connection between the AlmaUpdateDLQ and the AlmaErrorHandler.

### AlmaUpdater
Location: src/main/no.unit  
Location(handler): src/main/no.unit/alma/UpdateAlmaDescriptionHandler  
Entry point: AlmaUpdateQueue  
Exit point 1(success): ALMA(external database)  
Exit point 2(failure): AlmaUpdateDLQ  
Known failure 1: Record does not yet exist in ALMA.  
Know failure 2: Alma does not answer.  
Action(known failure 1): Send the record straight to AlmaUpdateDLQ, return null.  
Action(known failure 2): Attempts to communicate with ALMA will be retried 2 times before resulting in a thrown exception 
returning the record to the AlmaUpdateQueue, where it will be retried 2 more time before being sent to the AlmaUpdateDLQ.  
Purpose: Receive message from AlmaUpdateQueue. Extract the needed data from the message. 
Retrieve the relevant record from ALMA. Update the record from ALMA with extracted data.
Post the updated record back to ALMA. On unknown failures during execution the lambda will throw an exception,
sending the record back to AlmaUpdateQueue to be retried 2 more times before it is sent to the AlmaUpdateDLQ.


## Queues:

### AlmaUpdateQueue
Entry point 1: StreamReader  
Entry pont 2: AlmaErrorHandler  
Exit point 1: AlmaUpdater  
Exit point 2(failure\*): AlmaUpdateDLQ  
\* the queue will try to send the message to (exit point 1) 3 times before sending it to (exit point 2).

### AlmaUpdateDLQ
Entry point 1: AlmaUpdateQueue  
Entry point 2: AlmaUpdater  
Exit point: AlmaErrorHandler

### DynamoDbStreamDLQ 
Entry point: StreamReader

### AlmaUpdateExhausted
Entry point: AlmaErrorHandler

## Alarms:

### UpdateSchedulerAlarm
Monitoring: AlmaUpdateDLQ  
Trigger: Records in AlmaUpdate queue older than 5 days or no records in queue.  
Notify: AlmaErrorScheduler

