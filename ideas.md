# Ideas, the Monologue. Part 1
## TODO

- [ ] See TODOs below. Finish those
- [ ] Continue collecting points and things. Render non-horizontal upward planes in a different color. Make point cloud points a distinctive color.
- [ ] Make a 3 piece (front, left, right) TalkBack custom interface
- [ ] Make a first pass at persisting points over time using anchor trees. Every step discard untracked points, and consider future down points to be the minimum status.
- [ ] Ray trace (straight forward, straight left, straight right, etc)
- [ ] Connect the ray tracing with the TalkBack interface. This is the first ok demo point.
- [ ] Improve/fix the point preservation tree. This will eventually allow 360 ray tracing, even for points not currently in the user view.
- [ ] Simple ray tracing for checking each of the 5 positions.
- [ ] Improve the talkback interface to 5 regions.
- [ ] Look up an efficient way to ray trace (or something else) to see if a human could pass by accumulated point clouds. Estimate potential user paths and guess and check? Stochastically, if this is updating all the time, it should see something close enough.
- [ ] Implement the stochastic ray tracing to form a protective circle-cone around the user.
- [ ] Improve visual feedback to sighted users for demo beyond what was used for debugging purposes.

## Functionality
Look up what the app is supposed to provide as feedback. Planes? Point clouds with estimated distances and confidence levels?

Planes are helpful, because you can store the localization height relative to the lowest ground-parallel plane seen so far. This is the reference when looking for obstacles (higher planes or holes in the plane).

When given a plane, convert the point cloud into coordinates that are centered on the user and oriented relative to the floor. Look for groups of points that have been seen above the ground plane. These are also likely obstacles. Persist points that are out of view until there has been a scan through them to indicate that they are clear. Do 1ft, .25m voxels of the world to ease ray tracing and that kind of thing. Accumulate points in a region to estimate the occupation. 

### What does the example app do?
TODO Review models, images and textures

#### onCreate
- Setup config, verify the device is ready, create a session
- Setup gesture listener (use for vibrations management?, providing feedback when not with Talkback)
- Setup rendering

#### onSingleTap
- Store the single tap away for use when updating during drawing

#### onSurfaceCreated
- Implement the visualization of all the different rendered objects (planes, Andy, point clouds)
- This is setting up what every single surface/polygon should look like

#### onSurfaceChanged
- update the viewport and things I guess

#### onDrawFrame
- clear pixels from the last frame
- handle taps on the frame
- This uses API Frames, the view of everything and its valid relationship at the current point?
- Draw the background
- Check the frame tracking state. If it's not tracking, don't draw objects. Maybe render text to indicate its not tracking, and jump the TalkBack to that text?
- Get the projection matrix for rendering
- Get the camera matrix and draw. Not gonna lie, I don't know how these two relate, but I don't think it matters that much right now.
- Could I insert a model and draw the old key frames from anchors every so often to see the estimated position?
- Visualize tracked points (API Point Cloud(s))
- Display a "loading" message until at least one plane has been tracked. Use this as an example for displaying other messages to relay intermediate information.
- Visualize Planes using the triangular image
- Visualize touch-based anchors (where the Android sits right now)
- Are anchors relative to planes?
- Update the anchors relative to planes and redraw the Android model onto the screen (for each model in the for loop that is tracking)
- I can probably skip the shadows
- End by catching generic OpenGL exceptions to avoid throwing up. Display an extra little thing if there's an error?

### API Classes

#### Anchor
Describe a fixed location in the real world. Where is origin?
Anchors can either be tracked (accurately located), not currently tracked (poorly located), or stopped (discarded and never relocated)

Use anchors to save point clouds in a consistent way. This means that I'll have anchors with child features. If the anchor goes bad, discard all the child features.

#### Frame
"Provides a snapshot of AR state at a given timestamp"

#### Hit Result
Defines an intersection between a ray and estimated real world geometry. Ray trace to predict if the user will run into things?

#### Plane
Best knowledge of a real world planar surface.
Planes can be tracked in the same way as anchors (current, not current, stopped).
Planes can be horizontal-downward facing, horizontal-upward facing, or non-horizontal. Used for filtering based on binned normal vector.

Planes have bounding X, Z. Y axis is out of the plane. They also have more detailed polygons. These are measured relative to the plane's center.

#### Plane hit result
intersection between a ray and a plane

#### Point Cloud
A set of observed 3D points and confidence values

#### Point Cloud Hit Result
An intersection between a ray and a nearby point? This could be how I do ray tracing to clear open areas and detect obstacles.

#### Pose
An immutable rigid transformation from one coordinate frame to another.
Poses always describe the transformation from object's local coordinate frame to the world coods. Everything between frames should be measured from an Anchor. Numerical values are only valid for the given frame.

#### Session, Config
Manage and specify AR system things

## Behind the Scenes
Android Best Practices, later

## Interface Design Thesis and Summary

The interface will be a single page that takes advantage of built in Android accessiblity features (TalkBack) and potentially custom elements (rendering buttons/Talkback regions over the OpenGL interface). This may be supplanted by overlaying nearly transparent buttons from a different layout, although the custom stuff is the coolest. To start with, the app will not capture custom gestures, and will instead provide feedback via TalkBack and dynamic context descriptions.

The app will provide spoken feedback and vibrational feedback. For example: spoken feedback might say "There is an obstacle abou 10 feet in front of you". Vibrational feedback may vibrate lightly for a nearby frontal obstacle and more heavily for an obstacle that the user may run into soon.

The app interface will consist of 5 major regions. The center region is the largest and provides feedback about obstacles in a frontal cone in front of the user that are immediately relevant to the user. The left and right regions at the edge of the screen provide information about the cones to the left and right of the user. The top and bottom regions provide information about further in front of the user and behind the user respectively. The feedback falls into 3 categories: unknown/not seen/not processed, occupied or known free. Occupied regions provide feedback about the estimated distance to the obstacle.

There are two main ways of exploring content in the app using TalkBack. First is touch exploration. The user drags their finger on the screen. As UI elements are highlighted, they provide audio feedback. The second exploration method is linear exploration. Users swipe left and right to explore items in a linear ordering on the page (plus groupings). Touch exploration will allow for a full range of feedback. Linear exploration may only provide feedback about the left, right and front of the user.

A stretch goal is to identify humans and provide a heartbeat feedback when a person is near the user.

### Accesible Apps for Blind and Visually Impaired Users
TODO Review how to make a custom screen reader interface for OpenGL elements
https://www.youtube.com/watch?v=1by5J7c5Vz4

Talk Back Screen Reader

Users use preset gestures and get audio spoken words back as a result.

2 gesture input modes:
Touch exploration: drag finger across screen, getting feedback whenever you "hit" something/an item. Double tap to activate.
Linear Navigation: swipe left and right through selections until you find the item of interest. Double tap to activate.

Attach alternative text to UI elements to be spoken by TalkBack using android:contentDescription

Use "@null" to tell TalkBack to ignore the content description and not speak for that element

Don't include state or object description (ex. Button) in description because Android does this for you

Android Lint shows which content lacks descriptions

Arrange content into logical groups by using focusable containers, including grouping rows in tables for example. Then there is just one description for the group (unless it is selected, etc).

Manually test the app with Talkback, with eyes closed.

This is important.
When drawing app window with OpenGL, manually define Accessibility data. The easiest way to achieve this goal is to rely on the explore by touch helper class. This will help build a hierarchy of views that are accessible to Talkback.

https://developer.android.com/guide/topics/ui/accessibility/apps.html?utm_campaign=android_update_appsaccessibility_051717&utm_source=anddev&utm_medium=yt-desc

Content Description can be set in code. Use this to change the content description based on the region of the screen that the user is touching. For example, if their finger is on the center of the screen or dragged to the center of the screen, estimate the distance to an obstacle in front of the user. If they drag to the left or right, indicate if there are known obstacles in the left or right quadrant. Up and down indicate far forward or behind the user. 
This would be a good place to indicate a heartbeat if the app thinks there are people in each quadrant.

### Simple Gesture Detector
https://developer.android.com/reference/android/view/GestureDetector.SimpleOnGestureListener.html

This has a few simple options to handle. These can/will be used to make up a simple to use interface for interacting with the app without looking at the app (although if the app does pretty things while we're looking at it, that's great too for a demo).

- context click (equivalent to right click, long press to pull up a context menu)
- double tap
- double tap events (that occur during a double tap, ex. down, move and up events)
- down motion event
- fling?
- long press (how is this different from context click?)
- scroll (I don't think there's going to be scrolling in the app/single page app)
- show press (user has performed a down event and not moved or performed an up)
- single tap confirmed
- single tap up. Notified when a tap occurs with the up motion that triggered it

#### Down
This is the finger hitting the screen event I believe.

#### Up
This is the finger leaving the screen event I believe.

#### Double Tap
Duh

#### Double Tap Events
What is a move event inside a double tap? Move is a translation along the screen.

#### Fling
Touch the screen, move finger and release in a flick. Gives velocity in x and y for scrolling and that kind of thing.

#### Long Press
Duh. You get the initial on down event.

#### Scroll
Duh. When moving downed finger but not flicking it.

#### Show Press
What is a show press? Is it like a drill press? This is an event that goes off when the user goes down, but hasn't continued yet (release, move, etc). Use this to do something to indicate that the action was recognized, and I'm waiting for their finger to do something. ex highlight a button that is about to be clicked on release.

#### Single Tap Up vs. Confirmed
Confirmed is the input guarunteeing the single tap won't turn into a double tap. The up is the first indication that a single tap has happened, and may turn into a double tap.
