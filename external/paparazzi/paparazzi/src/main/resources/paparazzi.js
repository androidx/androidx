window.runs = {};

class Run {
  constructor(id, data) {
    this.id = id;
    // TODO(oldergod) which entries are required/optional?
    this.data = data;
  }
}

class Shot {
  constructor(name, test) {
    this.name = name;
    this.test = test;

    [, this.package, this.clazz, this.method] = Shot.TestMethodRegex.exec(test);

    this.runs = [];
  }

  static get TestMethodRegex() {
    return /^(.*)\.(.*)#(.*)$/;
  }

  addRun(runId, file, timestamp) {
    this.runs.push(
      {
        'id': runId,
        'file': file,
        'timestamp': timestamp
      }
    );

    if (file.endsWith('.png')) {
      this.img.src = file;
      this.img.style.display = 'inline';
      this.video.style.display = 'none';
    } else {
      this.video.src = file;
      this.video.style.display = 'inline';
      this.img.style.display = 'none';
    }
    this.timestampP.innerText = timestamp;

    const circle = document.createElement('div');
    circle.classList.add('test__details__selector', `run-${runId}`);
    circle.onmouseover = function (e) {
      if (file.endsWith('.png')) {
        this.img.src = file;
      } else {
        this.video.src = file;
      }

      for (let shot of Object.values(paparazziRenderer.shots)) {
        let found = false;
        for (let run of shot.runs) {
          if (runId == run.id) {
            shot.img.src = run.file;
            shot.timestampP.innerText = run.timestamp;

            found = true;
            break;
          }
        }
        shot.img.style.opacity = found ? "1" : "0.3";
      }
    }.bind(this);
    this.overlayDiv.appendChild(circle);
  }

  removeRun(runId) {
    const index = this.runs.indexOf((run) => run.id == runId);
    if (index == -1) return;

    this.runs.splice(index, 1);
  }

  inflate() {
    const screenDiv = document.createElement('div');
    screenDiv.classList.add('screen');

    document.rootContainer.appendChild(screenDiv);

    const img = document.createElement('img');
    const video = document.createElement('video');
    video.autoplay = 'autoplay';
    video.muted = 'muted';
    video.loop = 'loop';

    const overlayDiv = document.createElement('div');
    overlayDiv.classList.add('overlay');

    screenDiv.appendChild(img);
    screenDiv.appendChild(video);
    screenDiv.appendChild(overlayDiv);
    screenDiv.onmouseover = function (e) {
      overlayDiv.classList.add('overlay__hovered');
    }.bind(this);
    screenDiv.onmouseout = function (e) {
      overlayDiv.classList.remove('overlay__hovered');
    }.bind(this);

    const nameP = document.createElement('p');
    nameP.classList.add('test__details', 'test__details__name');

    const classP = document.createElement('p');
    classP.classList.add('test__details', 'test__details__class');

    const packageP = document.createElement('p');
    packageP.classList.add('test__details', 'test__details__package');

    const timestampP = document.createElement('p');
    timestampP.classList.add('test__details', 'test__details__timestamp');

    overlayDiv.appendChild(nameP);
    overlayDiv.appendChild(classP);
    overlayDiv.appendChild(packageP);
    overlayDiv.appendChild(timestampP);

    nameP.innerText = this.method;
    if (this.name !== undefined) {
      nameP.innerText += ` ${this.name}`;
    }
    classP.innerText = this.clazz;
    packageP.innerText = this.package;

    // hold references to the DOM for later updates
    this.img = img;
    this.video = video;
    this.timestampP = timestampP;
    this.overlayDiv = overlayDiv;
  }
}

class PaparazziRenderer {
  constructor() {
    // Used for content comparison for we only re-render the updated ones.
    this.currentRuns = {};
    // Used to store runs we know won't be updated anymore.
    this.lockedRunIds = [];
    this.shots = {}; // Key is `${test}${name}`, Value is a Shot.
  }

  start() {
    this.loadRunScript('index.js');
    for (let runId of window.all_runs) {
      this.loadRunScript(`runs/${runId}.js`);
    }
    setInterval(this.refresh.bind(this), 100);
  }

  render(run) {
    if (this.currentRuns[run.id]
      && JSON.stringify(this.currentRuns[run.id]) == JSON.stringify(run)) {
      // This run didn't change.
      return;
    }
    this.currentRuns[run.id] = run;
    console.log('rendering', run);

    for (let datum of run.data) {
      const key = `${datum.testName}${datum.name}`;
      let shot = this.shots[key];
      if (!shot) {
        console.log('New shot detected', shot);
        shot = new Shot(datum.name, datum.testName);
        this.shots[key] = shot;
        shot.inflate();
      } else {
        //shot.removeRun(run.id);
      }

      console.log('Adding run to shot', shot);
      shot.addRun(run.id, datum.file, datum.timestamp);

      // TODO setup listeners for filters/hovering, etc
    }
  }

  renderAll() {
    this.loadRunScript('index.js');
    for (let runId of window.all_runs) {
      if (this.lockedRunIds.includes(runId)) {
        continue;
      }
      // The js loading is async so the rendering can happen in the next refresh
      this.loadRunScript(`runs/${runId}.js`);

      this.render(new Run(runId, window.runs[runId]));

      const lastRunId = window.all_runs[window.all_runs.length - 1];
      if (runId != lastRunId) {
        // This run isn't the last run so we know it ain't gonna be updated.
        this.lockedRunIds.push(runId);
        delete this.currentRuns[runId];
      }
    }
  }

  refresh() {
    if (window.all_runs.length == 0) return;

    this.renderAll();
  }

  loadRunScript(js) {
    const script = document.createElement('script');
    script.src = js;
    script.onload = function () {
      this.remove();
    }
    document.head.appendChild(script);
  }
}

const paparazziRenderer = new PaparazziRenderer();
console.log(paparazziRenderer);

function bootstrap() {
  document.rootContainer = document.getElementById('rootContainer');
  paparazziRenderer.start();
}
