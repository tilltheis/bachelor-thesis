// auto-resize chart to always make it look nice
var chartContainer = document.getElementById("mostTweetedChart");
var svg = document.getElementsByTagName("svg")[0];
var globalLastStatistics;

function autoResizeChart() {
  var offsetTop = chartContainer.getBoundingClientRect().top;
  svg.style.height = Math.min(window.innerHeight - offsetTop, chartContainer.clientWidth / 2) + "px";
  svg.style.width = chartContainer.offsetWidth + "px";

  if (globalLastStatistics !== undefined) {
    makeMostTweetedChart("#mostTweetedChart svg", globalLastStatistics);
  }
}

window.onresize = autoResizeChart;
autoResizeChart();



// requires d3.layout.cloud.js

// most of this code is taken from https://github.com/jasondavies/d3-cloud/blob/c14b0c4d0a1bc08891b10fbeb443a23b063e7df9/examples/simple.html
// license: BSD
// statistics is a dictionary object { wourd: occurenceCount } like { "foo": 1, "bar": 2 }
// String x { String: Number } -> Unit
function makeMostTweetedChart(chartElementSelector, statistics) {
  var svg = document.querySelector(chartElementSelector);
  var width = parseInt(svg.style.width, 10);
  var height = parseInt(svg.style.height, 10);

  // { String: Number } -> [ { text: String, size: Number } ]
  function dataFromStatistics(statistics) {
    var data = [];

    for (var word in statistics) {
      if (statistics.hasOwnProperty(word)) {
        data.push({
          text: word,
          size: 9 + statistics[word] * 3
        });
      }
    }

    return data;
  }

  var fill = d3.scale.category20();

  // { text: String, size: Number } -> Unit
  function draw(words) {
    // completely rebuild
    d3.select(chartElementSelector + " g").remove();

    d3.select(chartElementSelector)
        .attr("width", width)
        .attr("height", height)
      .append("g")
        .attr("transform", "translate(" + (width/2) + "," + (height/2) + ")")
      .selectAll("text")
        .data(words)
      .enter().append("text")
        .style("font-size", function(d) { return d.size + "px"; })
        .style("font-family", "Impact")
        .style("fill", function(d, i) { return fill(i); })
        .attr("text-anchor", "middle")
        .attr("transform", function(d) {
          return "translate(" + [d.x, d.y] + ")rotate(" + d.rotate + ")";
        })
        .text(function(d) { return d.text; });
  }

  d3.layout.cloud().size([width, height])
    .words(dataFromStatistics(statistics))
    .padding(5)
    .rotate(function() { return ~~(Math.random() * 2) * 90; })
    .font("Impact")
    .fontSize(function(d) { return d.size; })
    .on("end", draw)
    .start();
}