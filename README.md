# Description
There are certain Faulty Storage Server. 
That server represents as server with new Api and the server with old API.

Old server's API:
- **GET <base-url>/oldStorage/files** returns a list of files in the old repository
- **GET <base-url>/oldStorage/files/{filename}** returns the content of the file with the specified name
- **DELETE <base-url>/oldStorage/files/{filename}** deletes the file with the specified name

New server's API:
- **GET <base-url>/newStorage/files** returns a list of files in the old repository
- **GET <base-url>/newStorage/files/{filename}** returns the content of the file with the specified name
- **DELETE <base-url>/newStorage/files/{filename}** deletes the file with the specified name
- **POST <base-url>/newStorage/files multipart/form-data** uploads file to a new storage

# Task
You need to use the API of this server to upload files from the old storage (oldStorage)
and upload them to the new storage (newStorage). 
At the same time, files from the old storage must be deleted.

It is worth remembering that the server is poor, 
so that all endpoints may unexpectedly respond with an error or freeze for some, 
albeit not very large, time.

As a result, you should get a console java application that implements migration.
