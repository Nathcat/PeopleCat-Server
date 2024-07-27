A Social Media platform for the future.

_PeopleCat © Nathcat 2024_
## Usage

In order to run the server, download a compiled jar package from the releases page, and create the following

directory structure:
```
Assets/
Data/
MySQL_Config.json
PeopleCat-Server
```

`MySQL_Config.json` should contain a JSON object which gives information
on how the server should connect to the required MySQL Server:

```json
{
	"connection_url_peoplecat": "tcp://hostname.of.server/PeopleCat",
	"username": "Username to connect with",
	"password": "Password to connect with"
}
```
## MySQL Setup
An SQL script to setup the MySQL is included in this repository: `PeopleCat_DB.sql`.