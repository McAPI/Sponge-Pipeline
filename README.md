## Protocol Documentation

##### Handshake
If you create a new connection to a server which is using the Pipeline, the server will
immediately return a packet with the following syntax.
```
PIPELINE {version} {key}
```
 - `PIPELINE` --> Is just a string without any significance.
 - `{version}` --> Is the used plugin version.
 - `{key}` --> Is the key that you've to use to identify yourself, in the current session. The {key} will expire as soon as the session is closed.
 
##### Your Response
You have to resspond to the server with a message. The message should be a JSON-String containg the following data:
```json
{"signature":"{signature}","payload":"{\"key\":\"{key}\"}"}
```

- `{signature}` is a HMAC-SHA256
 digest of the payload with the signature provided by the owner.  
- `payload` contains a JSON-encoded string with a key attribute that contains the session key.
 
##### Pipeline Response
The Pipeline will respond with two packets.

1. This package contains a JSON-String with only one attribute called "length". - This gives you the total length of the upcoming package. The length is in bytes. `{"length":2213}`
 
2. This package contains the real requested data. 
