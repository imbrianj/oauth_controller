/**
 * App Endpoint API Access Example
 *
 * Author: SmartThings, brian@bevey.org
 *
 * Taken almost verbatim from https://gist.github.com/aurman/9813279
 *
 * Creates OAuth endpoints for devices and modes set up on your SmartThings
 * account.
 */

definition(
    name: "OAuth Endpoint",
    namespace: "ImBrian",
    author: "Brian J.",
    description: "OAuth endpoint for Universal Controller",
    iconUrl: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience.png",
    iconX2Url: "https://s3.amazonaws.com/smartapp-icons/Convenience/Cat-Convenience%402x.png",
    oauth: true
)

preferences {
  section("Allow Endpoint to Control These Things...") {
    input "switches", "capability.switch", title: "Which Switches?", multiple: true, required: false
    input "locks",    "capability.lock",   title: "Which Locks?",    multiple: true, required: false
    input "hodor",    "capability.sensor", title: "Hodor?",          multiple: true, required: false
  }
}

mappings {
  path("/switches") {
    action: [
      GET: "listSwitches"
    ]
  }

  path("/switches/:id") {
    action: [
      GET: "showSwitch"
    ]
  }

  path("/switches/:id/:command") {
    action: [
      GET: "updateSwitch"
    ]
  }

  path("/locks") {
    action: [
      GET: "listLocks"
    ]
  }

  path("/locks/:id") {
    action: [
      GET: "showLock"
    ]
  }

  path("/locks/:id/:command") {
    action: [
      GET: "updateLock"
    ]
  }

  path("/mode/:mode") {
    action: [
      GET: "updateMode"
    ]
  }

  path("/hodor") {
    action: [
      GET: "hodor"
    ]
  }
}

def installed() {}

def updated() {}

// Switches
def listSwitches() {
  switches.collect{device(it,"switch")}
}

def showSwitch() {
  show(switches, "switch")
}

def updateSwitch() {
  update(switches, "switch")
}

// Locks
def listLocks() {
  locks.collect{device(it,"lock")}
}

def showLock() {
  show(locks, "lock")
}

def updateLock() {
  update(locks, "lock")
}

// Hodor
def hodor() {
  hodor.hodor()
}

// Modes
private updateMode() {
  log.debug "Mode change request: params: ${params}"

  def newMode = params.mode
  setLocationMode(newMode)
}

def deviceHandler(evt) {}

private update(devices, type) {
  def command  = params.command
  def device   = devices.find { it.id == params.id }
  def newValue = ''

  if (command) {
    if (!device) {
      httpError(404, "Device not found")
    }

    else {
      if(command == "toggle") {
        if(device.currentValue(type) == "on") {
          device.off();
          newValue = "off"
        }

        else {
          device.on();
          newValue = "on"
        }
      }

      else {
        device."$command"()
        newValue = command
      }
    }
  }

  [id: device.id, label: device.displayName, value: newValue, type: type]
}

private show(devices, type) {
  def device = devices.find { it.id == params.id }

  if (!device) {
    httpError(404, "Device not found")
  }

  else {
    def attributeName = type == "motionSensor" ? "motion" : type
    def s = device.currentState(attributeName)

    [id: device.id, label: device.displayName, value: s?.value, unitTime: s?.date?.time, type: type]
  }
}

private device(it, type) {
  it ? [id: it.id, label: it.label, type: type, state: it.currentValue(type)] : null
}
