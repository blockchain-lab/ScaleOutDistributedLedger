$(document).ready(function() {
    var source = new EventSource("../topn/updates");
    source.onmessage = function(event) {
        var data = JSON.parse(event.data);
        // $(".odometer").text(counter);
        network.setData({nodes: data.nodes, edges: data.edges});
        network.redraw();
        $(".transactions").text(data.numbers.numberOfTransactions);
        $(".chains").text(data.numbers.averageNumberOfChains);
        $(".blocks").text(data.numbers.averageNumberOfBlocks);
    };

    window.odometerOptions = {
        auto: false,
        format: '(,ddd).ddd', // Change how digit groups are formatted, and how many digits are shown after the decimal point
        duration: 100,
        animation: 'count'
    };

    var nodes = [];
    var edges = [];
    var network = null;

    function draw() {

        // Instantiate our network object.
        var container = document.getElementById('demo-graph');
        var data = {
            nodes: nodes,
            edges: edges
        };
        var options = {
            nodes: {
                shape: 'dot'
            },
            edges: {
                type: 'discrete'
            },
            layout: {
                randomSeed: 2
            },
            pysics: {
                barnesHut: {
                    springLength: 1000
                }
            }
            configure: {
                filter:function (option, path) {
                    if (path.indexOf('physics') !== -1) {
                        return true;
                    }
                    if (path.indexOf('smooth') !== -1 || option === 'smooth') {
                        return true;
                    }
                    return false;
                },
                container: document.getElementById('config')
            }
        };
        network = new vis.Network(container, data, options);
    }
    draw();

    var counter = 12;

    $("#graph_button").on('click', function() {
        edges[0].value += 1;
        counter += 1;
        $(".odometer").text(counter);
        network.setData({nodes: nodes, edges: edges});
        network.redraw();
    });
});