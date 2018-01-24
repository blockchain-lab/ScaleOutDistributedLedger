import bodyParser from 'body-parser';
import cookieParser from 'cookie-parser';
import Debug from 'debug';
import logger from 'morgan';
import express from 'express';
import http from 'http';
import index from './routes/index';
import NodeList from './model/NodeList';
import TransactionList from './model/TransactionList';
import path from "path";
const sseMW = require('./helpers/sse');

const app = express();
const debug = Debug('test:app');

app.use(sseMW.sseMiddleware);

app.use(logger('dev'));
app.use(bodyParser.json());
app.use(bodyParser.urlencoded({
	extended: false
}));
app.set('views', path.join(__dirname, '../views'));
app.set('view engine', 'ejs');
app.use(express.static(path.join(__dirname, '../public')));

const port = normalizePort(process.env.PORT || '3000');
app.set('port', port);
const server = http.createServer(app);

app.use(cookieParser());

app.nodeList = new NodeList();
app.transactionList = new TransactionList();
app.use('/', index);

server.listen(port);
server.on('listening', onListening);

// catch 404 and forward to error handler
app.use((req, res, next) => {
	const err = new Error('Not Found');
	err.status = 404;
	next(err);
});

// error handler
/* eslint no-unused-vars: 0 */
app.use((err, req, res, next) => {
	console.log(err);
	// set locals, only providing error in development
	res.locals.message = err.message;
	res.locals.error = req.app.get('env') === 'development' ? err : {};
	// render the error page
	res.status(err.status || 500);
	res.json(err);
});

// Handle uncaughtException
process.on('uncaughtException', (err) => {
	debug('Caught exception: %j', err);
	process.exit(1);
});

/**
 * Normalize a port into a number, string, or false.
 */

function normalizePort(val) {
	let port = parseInt(val, 10);

	if (isNaN(port)) {
		// named pipe
		return val;
	}

	if (port >= 0) {
		// port number
		return port;
	}

	return false;
}

/**
 * Event listener for HTTP server "listening" event.
 */
function onListening() {
	const addr = server.address();
	const bind = typeof addr === 'string'
		? 'pipe ' + addr
		: 'port ' + addr.port;
	console.log('Listening on ' + bind);
}

export default app;
