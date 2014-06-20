/**
 * Copyright (c) 2014 brian@bevey.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to
 * deal in the Software without restriction, including without limitation the
 * rights to use, copy, modify, merge, publish, distribute, sublicense, and/or
 * sell copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * 
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS
 * IN THE SOFTWARE.
 */

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
  switches.collect{showDevice(it,"switch",null,null)}
}

def showSwitch() {
  show(switches, "switch")
}

def updateSwitch() {
  update(switches, "switch")
}

// Locks
def listLocks() {
  locks.collect{showDevice(it,"lock",null,null)}
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

  switches.collect{showDevice(it, type, device, newValue)}
}

private show(devices, type) {
  def device = devices.find { it.id == params.id }

  if (!device) {
    httpError(404, "Device not found")
  }

  else {
    def attributeName = type == "motionSensor" ? "motion" : type
    def s = device.currentState(attributeName)

    [id: device.id, label: device.displayName, type: type, state: s?.value, unitTime: s?.date?.time]
  }
}

private showDevice(it, type, lastDevice, lastValue) {
  def deviceValue = ''

  if(it == lastDevice) {
    deviceValue = lastValue
  }

  else {
    deviceValue = it.currentValue(type)
  }

  it ? [id: it.id, label: it.label, type: type, state: deviceValue] : null
}
