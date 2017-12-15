import express from 'express';
const router = express.Router();
import Node from '../model/Node';
import app from '../app';

/**
 * Gets all nodes.
 */
router.get('/', (req, res) => {
    res.json({
        nodes: app.nodeList.getNodes()
    });
});

/**
 * Updates a certain node. Body should contain id, address and port.
 */
router.post('/update-node', (req, res) => {
    if(!isPresent(req.body.id) || !isPresent(req.body.address) || !isPresent(req.body.port)) {
        res.status(403);
        res.json({success: false, err: 'Specify id, address and port'});
    } else {
        const node = new Node(req.body.id, req.body.address, req.body.port);
        if (app.nodeList.updateNode(node)) {
            res.json({success: true});
        } else {
            res.status(403);
            res.json({success: false, err: 'invalid node'});
        }
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

function isPresent(arg) {
    return !!(arg || arg === 0 || arg === "" || arg === false);
}

export default router;
