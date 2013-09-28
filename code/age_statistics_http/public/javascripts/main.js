// requires nv.d3.js and underscore.js

// statistics is a dictionary object { age: count } like { 6: 1, 10: 2 }
function makeAgeStatisticsChart(chartElementSelector, statistics) {

  function dataFromStatistics(statistics) {
    function sum(xs) { return _.reduce(xs, function(sum, x) { return sum + x; }, 0); }
    return _.reduce(_.range(0, 100, 10), function(data, i) {
      var value = sum(_.values(_.filter(statistics, function(_count, age) {
        return age >= i && age <= i + 9;
      })));
      return data.concat([{
        label: i + "-" + (i + 9),
        value: value
      }]);
    }, []);
  }

  var data = dataFromStatistics(statistics);

  function dataFunction() {
    return [{
      key: "Age Statistics",
      values: data
    }];
  }

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


    chart.yAxis.axisLabel("#Votes").tickFormat(d3.format(",.0f"));
    chart.xAxis.axisLabel("Age (in Years)");

    // long bars will be cut off if domain is not set explicitly
    chart.yDomain([0, d3.max(data, function (d) { return d.value; })]);

    d3.select(chartElementSelector)
        .datum(dataFunction)
      .transition().duration(500)
        .call(chart);

    nv.utils.windowResize(chart.update);

    return chart;
  });

  return chart;
}