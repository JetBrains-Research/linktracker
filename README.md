# Tracking Changes in Links to Code 

This is a software project held in the collaboration of JetBrains and TU Delft. We are a group of 4 students working on this project. 

## Problem

Software documentation has an important role in software projects. 
There are many links, references in these documentations. 
Most of us do not update the documentation after each change to the 
project and it causes numerous invalid links and references in time. 
That brings along poor documentation and lower quality in product development.

## Our Solution

We are planning to build a system-agnostic, language-independent component. 
It will be capable of tracking changes in directories, files and 
lines that are being referenced by links inside of descriptions. 
Also, it will check whether the links are still valid or not after changes are applied. 
In the case where the link is not valid anymore, the component should suggest 
a new link that would correspond to the new state of the element, 
where this is applicable (the element could have been removed).

The component should be easy to integrate into all relevant environments, 
be programming-language independent and scale up to 
projects with a large number of links and files.

## Contributors
* [Ceren Ugurlu](https://gitlab.ewi.tudelft.nl/cugurlu)
* [Irem Ugurlu](https://gitlab.ewi.tudelft.nl/iugurlu)
* [Tommaso Brandirali](https://gitlab.ewi.tudelft.nl/tbrandirali)
* [Tudor Popovici](https://gitlab.ewi.tudelft.nl/tudorpopovici) 

<img align="right" src="https://user-images.githubusercontent.com/47633984/80767259-cdce2700-8b47-11ea-90a4-cf27fd585449.png" hspace="20" width="150"/>
<br/><br/>
<img align="right" src="https://user-images.githubusercontent.com/47633984/80767220-b68f3980-8b47-11ea-8125-f338980623d2.png" hspace="20" width="150"/>


<br/><br/><br/><br/><br/>