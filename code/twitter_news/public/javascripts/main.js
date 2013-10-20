


// auto-resize chart to always make it look nice
var chartContainer = document.getElementById("mostTweetedChart");
var svg = document.getElementsByTagName("svg")[0];

function autoResizeChart() {
  var offsetTop = chartContainer.getBoundingClientRect().top;
  var dimensionValue = Math.min(window.innerHeight - offsetTop, chartContainer.clientWidth);
  var value = window.innerWidth/2 + "px";
  svg.style.width = value;
  svg.style.height = value;
}

window.onresize = autoResizeChart;
autoResizeChart();



// requires nv.d3.js

// statistics is a dictionary object { age: count } like { 6: 1, 10: 2 }
function makeMostTweetedChart(chartElementSelector, statisticsParameter) {


  // [ { label: "0-9", value: 9 } ]
  function dataFromStatistics(statistics) {
    var data = [];

    for (var word in statistics) {
      if (statistics.hasOwnProperty(word)) {
        data.push({
          label: word,
          value: statistics[word]
        });
      }
    }

    // sort highest occurences first
    data.sort(function(x, y) {
      return y.value - x.value;
    });

    return data;
  }

  function dataFunction() {
    return [{
      key: "Most Tweeted",
      values: data
    }];
  }

  function draw() {
    // long bars will be cut off if domain is not set explicitly
    if (data !== []) {
        chart.yDomain([0, d3.max(data, function (d) { return d.value; })]);
    } else {
        chart.yDomain(0)
    }

    d3.select(chartElementSelector)
      .datum(dataFunction)
      .transition().duration(500)
      .call(chart);
  }

  function clone(o) {
    var result = {};
    for (var k in o) {
      if (o.hasOwnProperty(k)) { result[k] = o[k]; }
    }
    return result;
  }

  var statistics = clone(statisticsParameter);
  var data = dataFromStatistics(statistics);
  var chart;

  // see http://nvd3.org/ghpages/discreteBar.html
  nv.addGraph(function() {
    chart = nv.models.discreteBarChart()
      .x(function(d) { return d.label; })
      .y(function(d) { return d.value; })
      .staggerLabels(true)
      .tooltips(false)
      .showValues(true)
      .margin({left: 100})
      .margin({bottom: 100})
      .valueFormat(d3.format(",.0f"));

    chart.yAxis.axisLabel("#Occurences").tickFormat(d3.format(",.0f"));
    chart.xAxis.axisLabel("Words");

    draw();

    nv.utils.windowResize(chart.update);

    return chart;
  });

  return {
    increment: function(age) {
      var oldValue = statistics.hasOwnProperty(age) ? statistics[age] : 0;
      statistics[age] = oldValue + 1;
      data = dataFromStatistics(statistics);
    },
    setData: function(statistics) {
      data = dataFromStatistics(statistics);
    },
    update: draw
  }
}