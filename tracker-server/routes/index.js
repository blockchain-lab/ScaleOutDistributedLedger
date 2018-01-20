import express from 'express';
const router = express.Router();
import app from '../app';
import NodeList from '../model/NodeList';

/**
 * Gets all nodes.
 */
router.get('/', (req, res) => {
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

export default router;
