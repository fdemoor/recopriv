import matplotlib.pyplot as plt
import numpy as np
import sys
from os import chdir, getcwd


DATAFILENAME = "sybilAttack.csv"

#PERCENTS = [0.1, 0.2, 0.3, 0.5, 0.7, 0.8, 0.9]
PERCENTS = [0.1, 0.2, 0.3, 0.5, 0.7, 0.8, 0.9, 1.0]
#PERCENTS = [0.0, 0.01, 0.02, 0.03, 0.04, 0.05, 0.07, 0.08, 0.1, 0.2, 0.3, 0.5, 0.7, 0.8, 0.9, 1.0]

EXTRA_ITEMS = [5, 10, 15, 20, 25, 30, 35, 40, 45, 50]
REMOVAL_RATES = [0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.7, 0.8, 0.9, 1.0]
K = [1, 3, 5, 10, 15, 20, 25, 30, 40, 50, 75, 100]
FRAC = [2, 3, 4, 5, 6, 7, 8, 9, 10]

MARKERS = ['o', 's', '^', 'd', 'v', '*', '+']
COLORS = ['#FF8C00', '#008000', '#B22222', '#1E90FF', '#C71585', '#FF4500', '#8B4513']

KEEP_ONLY_SUPpI = False
#KEEP_ONLY_SUPpI = True
pI = [50, 50, 50, 50, 50, 50, 50, 50, 50, 50]
pI_ID = 0


def getParams():
  
  if len(sys.argv) != 7 and len(sys.argv) != 8:
      print("Invalid number of arguments")
      assert(False)
  BASEDIR = sys.argv[1]
  SUBDIRS = sys.argv[5].split(',')
  if len(sys.argv) == 8:
    ADDITIONNAL_PATH = sys.argv[7]
  else:
    ADDITIONNAL_PATH = ""
  YTYPES = sys.argv[3].split(',')
  XTYPE = sys.argv[2]
  CODE = int(sys.argv[4])
  LABELS = sys.argv[6].split(',')
  assert(len(LABELS) == len(SUBDIRS))
  SUBDIRS = [(SUBDIRS[i], LABELS[i]) if LABELS[i] != "" else (SUBDIRS[i], SUBDIRS[i]) for i in range(len(SUBDIRS))] 
  return BASEDIR, SUBDIRS, YTYPES, XTYPE, CODE, ADDITIONNAL_PATH


def getX(xType):
  fID = 0
  if xType == "auxPer":
    ABSCISSA = PERCENTS
    XLABEL = 'Fraction Auxiliary Items'
    DIRNAMES = "/percentAuxItems_"
  elif xType == "nbExtra":
    ABSCISSA = EXTRA_ITEMS
    XLABEL = 'Extra Items Number'
    DIRNAMES = "/nbExtraItems_"
  elif xType == "k":
    ABSCISSA = K
    XLABEL = 'k (nb neighbors)'
    DIRNAMES = "/k_"
  elif xType == "removalRate":
    ABSCISSA = REMOVAL_RATES
    XLABEL = 'Removal Rate'
    DIRNAMES = "/removalRate_"
  elif xType == "iPerNb":
    fID = 1
    ABSCISSA = []
    XLABEL = 'nbRemovedItems / nbAuxiliaryItems'
    DIRNAMES = ""
  elif xType == "bwFrac":
    ABSCISSA = FRAC
    XLABEL = 'fracBestItems: 1/x'
    DIRNAMES = "/bestWorstFrac_"
  else:
    print("Invalid ABSCISSA argument")
    assert(False)
  return ABSCISSA, XLABEL, DIRNAMES, fID


def getY(yType):
  YMIN = 0
  YMAX = 1
  if yType == "expectedNeighborhoods":
    ftransform = transformInfoDefaultBuilder(4)
    YLABEL = 'Expected neighborhoods'
  elif yType == "yield":
    ftransform = transformInfoDefaultBuilder(1)
    YLABEL = 'Yield'
    YMAX = 40
  elif yType == "accuracy":
    ftransform = transformInfoDefaultBuilder(2)
    YLABEL = 'Accuracy'
  elif yType == "infiltration":
    ftransform = transformInfoDefaultBuilder(0)
    YLABEL = 'Sybil infiltration' 
  elif yType == "sybN":
    ftransform = transformInfoDefaultBuilder(6)
    YLABEL = 'Sybil neighbors'
  elif yType == "PSC":
    ftransform = transformInfoDefaultBuilder(9)
    YLABEL = 'Perfectly similar counterparts'
    YMAX = 18
  elif yType == "TiN":
    ftransform = transformInfoDefaultBuilder(5)
    YLABEL = 'Target is neighbor'
  elif yType == "AxY":
    ftransform = lambda l: transformInfoDefaultBuilder(1)(l) * transformInfoDefaultBuilder(2)(l)
    YLABEL = 'Accuracy * Yield' 
    YMAX = 6
  elif yType == "RMSE":
    global DATAFILENAME
    DATAFILENAME = 'recoQuality.csv'
    ftransform = transformInfoDefaultBuilder(4)
    YLABEL = 'RMSE' 
    YMIN = 1
    YMAX = 1.45
  else:
    print("Invalid ORDINATE argument")
    assert(False)
  return ftransform, YLABEL, YMIN, YMAX


def extractInfo(dir, ftransform):
    basedir = getcwd()
    chdir(dir)
    filepath = dir+"/"+DATAFILENAME
    datafile = open(filepath, "r")
    x, k = 0, 0
    for line in datafile:
      splitList = line.split(",")
      if KEEP_ONLY_SUPpI:
        n = transformInfoDefaultBuilder(12)(splitList) - pI[pI_ID]
      if (not KEEP_ONLY_SUPpI) or n >= 0:
        x += ftransform(splitList)
        k += 1
    datafile.close()
    chdir(basedir)
    if k == 0:
      return x, 0
    return x / k, k
    
def extractInfoPer(dir, ftransform):
    targetIDs = []
    basedir = getcwd()
    chdir(dir)
    filepath = dir+"/"+DATAFILENAME
    datafile = open(filepath, "r")
    results = []
    occ = []
    targetiId = 0
    for line in datafile:
      targetiId += 1
      splitList = line.split(",")
      e = [ftransform(splitList), int(transformInfoDefaultBuilder(12)(splitList))]
      if KEEP_ONLY_SUPpI:
        n = transformInfoDefaultBuilder(12)(splitList) - pI[pI_ID]
      if (not KEEP_ONLY_SUPpI) or n >= 0:
        try:
          i = results.index(e)
          occ[i] += 1
          targetIDs[i].append(targetiId)
        except ValueError:
          results.append([ftransform(splitList), int(transformInfoDefaultBuilder(12)(splitList))])
          occ.append(1)
          targetIDs.append([targetiId])
    datafile.close()
    chdir(basedir)
    
    x = [pI[pI_ID] / e[1] for e in results]
    y = [e[0] for e in results]
    
    return x, y, occ, targetIDs
  
  
def transformInfoDefaultBuilder(i):
  return lambda splitList: float(splitList[i])


def plot(xType, yType, ax, BASEDIR, ADDITIONNAL_PATH, SUBDIRS):
  
  global pI_ID
  
  ABSCISSA, XLABEL, DIRNAMES, fID = getX(xType)
  ftransform, YLABEL, YMIN, YMAX = getY(yType)
  
  ORDINATE = []
  
  if fID == 0:
    pI_ID = 0
    for DIR in SUBDIRS:
      XMIN = min(ABSCISSA)
      #XMIN = 0 #
      XMAX = max(ABSCISSA)
      X = np.array([extractInfo(BASEDIR+"/"+DIR[0]+ADDITIONNAL_PATH+DIRNAMES+str(i), ftransform) for i in ABSCISSA])
      ORDINATE.append([DIR[1], [e[0] for e in X], [e[1] for e in X]])
      pI_ID += 1
  elif fID == 1:
    for DIR in SUBDIRS:
      ABSCISSA, X, Z, targetIDs = extractInfoPer(BASEDIR+"/"+DIR[0]+ADDITIONNAL_PATH, ftransform)
      XMIN = min(ABSCISSA)
      XMAX = max(ABSCISSA)
      ORDINATE.append([DIR[1], X, Z])
      XMIN = -0.1
      XMAX = 1.1
      YMIN -= 0.1
      YMAX += 0.1
      
      print([targetIDs[i] for i in range(len(Z)) if ABSCISSA[i] <= 0.2 and X[i] <= 0.2])
    
  else:
    assert(false)
  
  

  marker, color = 0, 0
  for X in ORDINATE:
      if fID == 0:
        plt.plot(ABSCISSA, X[1], color=COLORS[color], label=X[0], marker=MARKERS[marker])
        if KEEP_ONLY_SUPpI:
          #for i in range(len(X[1])):
            #plt.annotate(str(int(X[2][i])), xy=(ABSCISSA[i], X[1][i]))
          print(YLABEL + X[0] + ": Remaining nodes:", X[2])
        
        ax.legend(loc=1, frameon=False, numpoints=1, markerfirst=False)# fontsize=9)
        
      elif fID == 1:
        plt.scatter(ABSCISSA, X[1], c=X[2], cmap='gist_rainbow', label=X[0], marker=MARKERS[marker])
        plt.colorbar()
      marker = (marker + 1) % len(MARKERS)
      color = (color + 1) % len(COLORS)

  ax.axis([XMIN, XMAX, YMIN, YMAX])
  ax.set_xlabel(XLABEL)
  ax.set_ylabel(YLABEL)
  ax.grid()
  #plt.xticks(np.arange(XMIN, XMAX+1, 10)) #
  #plt.yticks(np.arange(YMIN, YMAX+0.01, 0.05)) #
  

def main():
  
  BASEDIR, SUBDIRS, YTYPES, XTYPE, CODE, ADDITIONNAL_PATH = getParams()
  chdir(BASEDIR)
  
  NH, NW = CODE // 100, (CODE % 100) // 10
  
  fig = plt.figure()
  for i in range(len(YTYPES)):
    ax = fig.add_subplot(CODE + i + 1)
    plot(XTYPE, YTYPES[i], ax, BASEDIR, ADDITIONNAL_PATH, SUBDIRS)
  #fig.tight_layout()
  fig.set_size_inches(NW * 7, NH * 4)
  fig.savefig(ADDITIONNAL_PATH[1:]+'_plot.svg', bbox_inches='tight')
  


main()

## MATPLOTLIB LEGEND LOCATION ##
#'upper right'  : 1,
#'upper left'   : 2,
#'lower left'   : 3,
#'lower right'  : 4,
#'right'        : 5,
#'center left'  : 6,
#'center right' : 7,
#'lower center' : 8,
#'upper center' : 9,
#'center'       : 10,
