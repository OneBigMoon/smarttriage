'use strict';

const cmapData = {
  center: [104.114129, 37.550339],
  zoom: 8,
  roam: true,
  mapStyle: {
    styleJson: [{
      featureType: 'water',
      elementType: 'all',
      stylers: {
        color: '#A6C2DE'
      }
    }, {
      featureType: 'land',
      elementType: 'all',
      stylers: {
        color: '#F5F3F0'
      }
    }, {
      featureType: 'boundary',
      elementType: 'geometry',
      stylers: {
        color: '#BAAB78'
      }
    }, {
      featureType: 'railway',
      elementType: 'all',
      stylers: {
        visibility: 'off'
      }
    }, {
      featureType: 'highway',
      elementType: 'geometry',
      stylers: {
        color: '#fdfdfd'
      }
    }, {
      featureType: 'highway',
      elementType: 'geometry.fill',
      stylers: {
        color: '#fdfdfd',
        lightness: 1
      }
    }, {
      featureType: 'highway',
      elementType: 'labels',
      stylers: {
        visibility: 'off'
      }
    }, {
      featureType: 'arterial',
      elementType: 'geometry',
      stylers: {
        color: '#fefefe'
      }
    }, {
      featureType: 'arterial',
      elementType: 'geometry.fill',
      stylers: {
        color: '#fefefe'
      }
    }, {
      featureType: 'poi',
      elementType: 'all',
      stylers: {
        visibility: 'off'
      }
    }, {
      featureType: 'green',
      elementType: 'all',
      stylers: {
        color: '#D5D5D5',
        visibility: 'off'
      }
    }, {
      featureType: 'subway',
      elementType: 'all',
      stylers: {
        visibility: 'off'
      }
    }, {
      featureType: 'manmade',
      elementType: 'all',
      stylers: {
        visibility: 'off'
      }
    }, {
      featureType: 'local',
      elementType: 'all',
      stylers: {
        visibility: 'off'
      }
    }, {
      featureType: 'arterial',
      elementType: 'labels',
      stylers: {
        visibility: 'off'
      }
    }, {
      featureType: 'boundary',
      elementType: 'geometry.fill',
      stylers: {
        color: '#BAAB78'
      }
    }, {
      featureType: 'building',
      elementType: 'all',
      stylers: {
        color: '#d1d1d1'
      }
    }, {
      featureType: 'label',
      elementType: 'all',
      stylers: {
        visibility: 'off'
      }
    }]
  }
};


const convertType = {
  ac: '客流量',
  awd: '排队时长',
  atd: '办理时长',
  lp: '负荷率'
};

function formateSerie(name, type, symbol, symbolSize, zlevel, color, showEffectOn, data) {
  const serie = {
    name,
    type,
    coordinateSystem: 'bmap',
    symbolSize,
    itemStyle: {
      normal: {
        color
      }
    },
    zlevel,
    data
  };
  if (type == 'scatter') {
    serie.label = {
      normal: {
        show: true,
        textStyle: {
          color: '#fff',
          fontSize: 13
        },
        formatter(params) {
          if (params.seriesName == '负荷率') {
            return `${Math.round(params.value[2] * 100) / 100}`;
          }
          if (params.seriesName == '客流量') {
            return `${params.value[2]}`;
          }
          let r = '';
          let i = params.value[2];
          if (Math.floor(i / 1000 % 60) > 0) {
            r = Math.floor(i / 1000 % 60);
          }
          i = Math.floor(i / 1000 / 60);
          if (i > 0 && (i % 60 > 0)) {
            r = i % 60 + ':' + (r ? r : '00');
          }
          i = Math.floor(i / 60);
          if (i > 0) {
            r = i + ':' + (r ? r : '00');
          }
          return (r === '') ? '0' : r;
        }
      }
    };
  } else {
    serie.effectType = 'ripple';
    serie.label = { normal: { show: true, position: 'right', formatter: '{b}' } };
  }
  if (symbol) {
    serie.symbol = symbol;
  }
  if (showEffectOn) {
    serie.showEffectOn = showEffectOn;
  }
  return serie;
}


function convertOfficeSeriesData(name, dangerData, warnningData, normalData, totalData, size) {
  const series = [];
  series.push(formateSerie(name, 'scatter', 'pin', size, 25, '#F62157', undefined, dangerData));
  series.push(formateSerie(name, 'scatter', 'pin', 50, 25, '#F35006', undefined, warnningData));
  series.push(formateSerie(name, 'scatter', 'pin', 50, 25, '#C2AE09', undefined, normalData));
  series.push(formateSerie(name, 'effectScatter', undefined, 5, 3, '#036FA5', 'render', totalData));
  return series;
}
function getProvinceSeries(type, provinceData, data) {
  let size = 70;
  if (type == 'awd' || type == 'atd') {
    size = 60;
  }
  return convertOfficeSeriesData(`${convertType[type]}`, provinceData, [{}, {}, {}, {}], [{}, {}, {}, {}], data, size);
}

function getCitySeries(type, cityData, data) {
  let size = 70;
  if (type == 'awd' || type == 'atd') {
    size = 80;
  }
  return convertOfficeSeriesData(`${convertType[type]}`, cityData, [{}, {}, {}, {}], [{}, {}, {}, {}], data, size);
}

function getOfficeSeries(type, officeData, data) {
  let size = 70;
  if (type == 'awd' || type == 'atd') {
    size = 80;
  }
  return convertOfficeSeriesData(`${convertType[type]}`, officeData, [{}, {}, {}, {}], [{}, {}, {}, {}], data, size);
}
module.exports = {
  cmapData,
  formateSerie,
  getProvinceSeries,
  getCitySeries,
  getOfficeSeries
};
