import express from 'express';
const router = express.Router();
import app from '../app';
import NodeList from '../model/NodeList';

/**
 * Gets all nodes.
 */
router.get('/', (req, res) => {
    updateS
	res.json({
		nodes: app.nodeList.getNodes()
	});
});

/**
 * Updates a certain node. Body should contain id, address, port and publicKey.
 */
router.post('/update-node', (req, res) => {
	if(!isPresent(req.body.id) || !isPresent(req.body.address) || !isPresent(req.body.port) || !isPresent(req.body.publicKey)) {
		res.status(403);
		res.json({success: false, err: 'Specify id, address, port and publicKey'});
	} else {
		if (app.nodeList.updateNode(req.body.id, req.body.address, req.body.port, req.body.publicKey)) {
			res.json({success: true});
		} else {
			res.status(403);
			res.json({success: false, err: 'invalid node'});
		}
	}
});

/**
 * Register a new node, body should contain address, port and publicKey.
 */
router.post('/register-node', (req, res) => {
    if(!isPresent(req.body.address) || !isPresent(req.body.port) || !isPresent(req.body.publicKey) || !isPresent(req.body.id)) {
        res.status(403);
        res.json({success: false, err: 'Specify id, address, port and publicKey'});
    } else {
        const id = app.nodeList.registerNode(req.body.id, req.body.address, req.body.port, req.body.publicKey);
        res.json({success: true, id: id});
    }
});

/**
 * Update the running status of a node.
 */
router.post('/set-node-status', (req, res) => {
    if(!isPresent(req.body.id) || !isPresent(req.body.running)) {
        res.status(403);
        res.json({success: false, err: 'Specify node ID and running status'});
    } else {
        app.nodeList.setNodeStatus(req.body.id, req.body.running)
        res.json({success: true, id: req.body.id});
    }
});

/**
 * Gets a specific node. Getter body should contain id.
 */
router.get('/node', (req, res) => {
	if(!isPresent(req.body.id)) {
		res.status(403);
		res.json({success: false, err: 'Specify id'});
	} else {
		const node = app.nodeList.getNode(req.body.id);
		if (node == null) {
			res.status(403);
			res.json({success: false, err: 'invalid id or uninitialized node'});
		} else {
			res.json({success: true, node: node});
		}
	}
});

router.get('/demo', (req, res) => {
	res.render('demo');
});

/**
 * Reset the nodelist on the tracker server.
 */
router.post('/reset', (req, res) => {
	app.nodeList = new NodeList();
	res.json({success: true});
});

/**
 * Get the number of currently registered nodes and currently running nodes on the tracker server.
 */
router.get('/status', (req, res) => {
	res.json({registered: app.nodeList.getSize(), running:  app.nodeList.getRunning()});
});

function isPresent(arg) {
	return !!(arg || arg === 0 || arg === "" || arg === false);
}

var sseClients = new sseMW.Topic();
// initial registration of SSE Client Connection
app.get('/topn/updates', function(req,res){
    var sseConnection = res.sseConnection;
    sseConnection.setup();
    sseClients.add(sseConnection);
} );
var m;
//send message to all registered SSE clients
alrighfunction updateSseClients(message) {
    var msg = message;
    this.m=message;
    sseClients.forEach(
        function(sseConnection) {
            sseConnection.send(this.m);
        }
        , this // this second argument to forEach is the thisArg (https://developer.mozilla.org/en-US/docs/Web/JavaScript/Reference/Global_Objects/Array/forEach)
    ); //forEach
}// updateSseClients

// send a heartbeat signal to all SSE clients, once every interval seconds (or every 3 seconds if no interval is specified)
initHeartbeat = function(interval) {
    setInterval(function()  {
            var msg = {"label":"The latest", "time":new Date()};
            updateSseClients( JSON.stringify(msg));
        }//interval function
        , interval?interval*1000:3000
    ); // setInterval
}//initHeartbeat

// initialize heartbeat at 10 second interval
initHeartbeat(10);

export default router;
