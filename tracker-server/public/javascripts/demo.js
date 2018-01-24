$(document).ready(function() {
    var source = new EventSource("../topn/updates");
    source.onmessage = function(event) {
        var data = JSON.parse(event.data);
        // $(".odometer").text(counter);
        network.setData({nodes: data.nodes, edges: data.edges});
        network.redraw();
    };



    window.odometerOptions = {
        auto: false, // Don't automatically initialize everything with class 'odometer'
        // selector: '.my-numbers', // Change the selector used to automatically find things to be animated
        // format: '(,ddd).dd', // Change how digit groups are formatted, and how many digits are shown after the decimal point
        duration: 100, // Change how long the javascript expects the CSS animation to take
        // theme: 'car', // Specify the theme (if you have more than one theme css file on the page)
        // animation: 'count' // Count is a simpler animation method which just increments the value,
                           // use it when you're looking for something more subtle.
    };

    var nodes = [];
    var edges = [];
    var network = null;
    // create people.
    // value corresponds with the age of the person
    // nodes = [
    //     {id: 1,  value: 2,  label: 'Algie' },
    //     {id: 2,  value: 31, label: 'Alston'},
    //     {id: 3,  value: 12, label: 'Barney'},
    //     {id: 4,  value: 16, label: 'Coley' },
    //     {id: 5,  value: 17, label: 'Grant' },
    //     {id: 6,  value: 15, label: 'Langdon'},
    //     {id: 7,  value: 6,  label: 'Lee'},
    //     {id: 8,  value: 5,  label: 'Merlin'},
    //     {id: 9,  value: 30, label: 'Mick'},
    //     {id: 10, value: 18, label: 'Tod'},
    // ];
    //
    // // create connections between people
    // // value corresponds with the amount of contact between two people
    // edges = [
    //     {from: 2, to: 8, value: 3, title: '3 emails per week'},
    //     {from: 2, to: 9, value: 5, title: '5 emails per week'},
    //     {from: 2, to: 10,value: 1, title: '1 emails per week'},
    //     {from: 4, to: 6, value: 8, title: '8 emails per week'},
    //     {from: 5, to: 7, value: 2, title: '2 emails per week'},
    //     {from: 4, to: 5, value: 1, title: '1 emails per week'},
    //     {from: 9, to: 10,value: 2, title: '2 emails per week'},
    //     {from: 2, to: 3, value: 6, title: '6 emails per week'},
    //     {from: 3, to: 9, value: 4, title: '4 emails per week'},
    //     {from: 5, to: 3, value: 1, title: '1 emails per week'},
    //     {from: 2, to: 7, value: 4, title: '4 emails per week'}
    // ];

    function draw() {

        // Instantiate our network object.
        var container = document.getElementById('demo-graph');
        var data = {
            nodes: nodes,
            edges: edges
        };
        var options = {
            nodes: {
                shape: 'dot',
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