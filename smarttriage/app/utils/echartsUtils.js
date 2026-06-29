'use strict';

const {
  extend,
  each,
  pick,
  isObject
} = require('../utils');

const noDataLoadingOption = {
  text: '暂无数据',
  effect: 'bubble',
  effectOption: {
    effect: {
      n: 0
    },
    backgroundColor: 'transparent'
  },
  textStyle: {
    fontSize: 20
  }
};

const eUtils = module.exports = {
  isPieChart(type) {
    return ['pie', 'donut'].indexOf(type) > -1;
  },
  isMapChart(type) {
    return ['map'].indexOf(type) > -1;
  },
  isAxisChart(type) {
    return ['line', 'bar', 'area'].indexOf(type) > -1;
  },
  getAxisTicks(data, config, type, isX) {
    const ticks = [];
    if (data[0]) {
      each(data[0].datapoints, (datapoint) => {
        ticks.push(datapoint.x);
      });
    }

    if (config.yAxis && config.yAxis.type === 'category') {
      if (isX) {
        return {};
      }
    } else {
      if (!isX) {
        return {};
      }
    }
    return {
      type: 'category',
      boundaryGap: type === 'bar',
      data: ticks
    };
  },
  getSeries(data, config, type) {
    const series = [];
    each(data, (serie) => {
      // datapoints for line, area, bar chart
      const datapoints = [];
      each(serie.datapoints, (datapoint) => {
        datapoints.push(datapoint.y);
      });
      let conf = {
        type: type || 'line',
        name: serie.name,
        data: datapoints
      };
      // area chart is actually line chart with special itemStyle
      if (type === 'area') {
        conf.type = 'line';
        conf.itemStyle = { normal: { areaStyle: { type: 'default' } } };
      }
      if (type === 'bar') {
        conf = extend(conf, config.bar || {});
      }
      // gauge chart need many special config
      if (type === 'gauge') {
        conf = extend(conf, config.gauge || {});
      }

      if (type === 'funnel') {
        conf = extend(conf, {
          funnelAlign: 'right',
          sort: 'ascending'
        }, config.funnel || {});
      }
      // datapoints for pie chart and gauges are different
      if (!eUtils.isAxisChart(type)) {
        conf.data = [];
        each(serie.datapoints, (datapoint) => {
          conf.data.push({
            value: datapoint.y,
            name: datapoint.x
          });
        });
      }
      if (eUtils.isPieChart(type)) {
        // donut charts are actually pie charts
        conf.type = 'pie';
        // pie chart need special radius, center config
        conf.center = config.center || ['40%', '50%'];
        conf.radius = config.radius || '60%';
        // donut chart require special itemStyle
        if (type === 'donut') {
          conf.radius = config.radius || ['50%', '70%'];
          conf = extend(conf, config.donut || {});
        } else if (type === 'pie') {
          if (config.roseType) {
            conf.roseType = config.roseType;
          }
          const pieConf = extend({
            itemStyle: {
              normal: {
                label: {
                  show: true,
                  formatter: config.labelFormatter || undefined
                },
                labelLine: {
                  show: true,
                  length: 2
                }
              }
            }
          }, config.pie || {});
          conf = extend(conf, pieConf);
        }
      }
      if (eUtils.isMapChart(type)) {
        conf.type = 'map';
        conf = extend(conf, config.map || {});
      }
      // if stack set to true
      if (config.stack) {
        conf.stack = 'total';
      }
      series.push(conf);
    });
    return series;
  },
  getLegend(data, config, type) {
    const legend = { data: [] };
    if (eUtils.isPieChart(type)) {
      if (data[0]) {
        each(data[0].datapoints, (datapoint) => {
          legend.data.push(datapoint.x);
        });
      }
    } else {
      each(data, (serie) => {
        legend.data.push(serie.name);
      });
    }
    return extend(legend, config.legend || {});
  },
  getTooltip(data, config, type) {
    const tooltip = {};
    switch (type) {
      case 'line':
      case 'area':
        tooltip.trigger = 'axis';
        break;
      case 'pie':
      case 'donut':
      case 'bar':
      case 'map':
      case 'gauge':
        tooltip.trigger = 'item';
        break;
      default:
        break;
    }
    if (type === 'pie') {
      tooltip.formatter = '{a} <br/>{b}: {c} ({d}%)';
    }
    if (type === 'map') {
      tooltip.formatter = '{b}';
    }
    return extend(tooltip, isObject(config.tooltip) ? config.tooltip : {});
  },
  getTitle(data, config, type) {
    if (isObject(config.title)) {
      return config.title;
    }
    if (eUtils.isPieChart(type)) {
      return {
        text: config.title,
        subtext: config.subtitle || '',
        x: 'center'
      };
    }
    return '';
  },
  formatKMBT(y, formatter) {
    if (!formatter) {
      formatter = function(v) {
        return Math.round(v * 100) / 100;
      };
    }
    y = Math.abs(y);
    if (y >= 1000000000000) {
      return formatter(y / 1000000000000) + 'T';
    } else if (y >= 1000000000) {
      return formatter(y / 1000000000) + 'B';
    } else if (y >= 1000000) {
      return formatter(y / 1000000) + 'M';
    } else if (y >= 1000) {
      return formatter(y / 1000) + 'K';
    } else if (y < 1 && y > 0) {
      return formatter(y);
    } else if (y === 0) {
      return '';
    }
    return formatter(y);
  },
  getSizes(config, dom) {
    dom.style.width = (config.width || 320) + 'px';
    dom.style.height = (config.height || 240) + 'px';
  },
  getOptions(data, config, type) {
    if (type == 'radar') {
      const radarOptions = pick(config, ['title', 'tooltip', 'calculable', 'polar']);
      radarOptions.series = [{
        type,
        data: data[0].datapoints,
        itemStyle: { normal: { areaStyle: { type: 'default' } } }
      }];

      radarOptions.noDataLoadingOption = noDataLoadingOption;

      return radarOptions;
    }
    if (type == 'map' || type == 'bmap') { // 复杂对象直接赋值
      return extend(extend({}, config), data);
    }

    config = extend({
      showXAxis: true,
      showYAxis: true,
      showLegend: true,
      showGrid: true
    }, config);
    const grid = {
      borderColor: '#fff'
    };
    const xAxis = extend({
      // orient: 'top',
      // axisLine: { show: false }
    }, isObject(config.xAxis) ? config.xAxis : {});
    const yAxis = extend({
      type: 'value'
    }, isObject(config.yAxis) ? config.yAxis : {});
    // basic config
    const options = {
      title: eUtils.getTitle(data, config, type),
      tooltip: eUtils.getTooltip(data, config, type),
      legend: eUtils.getLegend(data, config, type),
      toolbox: extend({ show: false }, isObject(config.toolbox) ? config.toolbox : {}),
      xAxis: [extend(xAxis, eUtils.getAxisTicks(data, config, type, true))],
      yAxis: [extend(yAxis, eUtils.getAxisTicks(data, config, type, false))],
      series: eUtils.getSeries(data, config, type)
    };
    if (!config.showXAxis) {
      each(options.xAxis, (axis) => {
        axis.axisLine = { show: false };
        axis.axisLabel = { show: false };
        axis.axisTick = { show: false };
      });
    }
    if (!config.showYAxis) {
      each(options.yAxis, (axis) => {
        axis.axisLine = { show: false };
        axis.axisLabel = { show: false };
        axis.axisTick = { show: false };
      });
    }
    if (!config.showLegend || type === 'gauge' || type === 'map') {
      delete options.legend;
    }
    if (!eUtils.isAxisChart(type)) {
      delete options.xAxis;
      delete options.yAxis;
    }
    if (config.dataZoom) {
      options.dataZoom = extend({
        show: true,
        realtime: true
      }, config.dataZoom);
    }
    options.grid = grid;
    if (!config.showGrid || type === 'gauge' || type === 'map' || type === 'pie' || type === 'donut') {
      delete options.grid;
    }
    options.noDataLoadingOption = noDataLoadingOption;

    return options;
  }
};
