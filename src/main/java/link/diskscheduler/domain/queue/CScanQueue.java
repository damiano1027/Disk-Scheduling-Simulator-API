package link.diskscheduler.domain.queue;

import link.diskscheduler.domain.cylinder.Cylinder;
import link.diskscheduler.domain.cylinder.Cylinders;
import link.diskscheduler.domain.direction.ScanDirection;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class CScanQueue extends Queue {
    private static final int FRONT = 0;
    private final List<Cylinder> cylinders;

    public static CScanQueue create() {
        return new CScanQueue(new LinkedList<>());
    }

    @Override
    public void add(Cylinder cylinder) {
        cylinders.add(cylinder);
    }

    public void addFront(Cylinder cylinder) {
        cylinders.add(FRONT, cylinder);
    }

    @Override
    public boolean isEmpty() {
        return cylinders.isEmpty();
    }

    @Override
    public List<Cylinder> peekCurrentCylinders() {
        return cylinders;
    }

    public Cylinder getNextCylinderFrom(int currentHeadLocation, ScanDirection currentScanDirection, boolean isHeadOnTheWayBack) {
        if (isEmpty()) {
            return null;
        }

        Cylinder nextCylinder = null;

        // circular scan 을 위해 처음 or 끝 실린더로 복귀중이라면
        if (isHeadOnTheWayBack) {
            nextCylinder = cylinders.get(FRONT);

            ScanDirection reversedDirection = currentScanDirection.reverse();

            for (int i = 1; i < cylinders.size(); i++) {
                Cylinder cylinder = cylinders.get(i);

                if ((reversedDirection.equals(ScanDirection.LEFT) && cylinder.isNumberGreaterThan(nextCylinder))
                        || (reversedDirection.equals(ScanDirection.RIGHT) && cylinder.isNumberLessThan(nextCylinder))) {
                    nextCylinder = cylinder;
                }
            }
        }
        // 복귀중이 아니라면
        else {
            List<Cylinder> cylindersInRange = new ArrayList<>();
            List<Cylinder> cylindersOutRange = new ArrayList<>();

            cylinders.forEach(cylinder -> {
                if (isCylinderInRange(cylinder, currentHeadLocation, currentScanDirection)) {
                    cylindersInRange.add(cylinder);
                } else {
                    cylindersOutRange.add(cylinder);
                }
            });

            // 현재 scan 방향에 있는 요청 실린더가 있다면, 현재 위치와 가장 가까운 요청 실린더를 추출
            if (!cylindersInRange.isEmpty()) {
                nextCylinder = cylindersInRange.get(0);
                for (int i = 1; i < cylindersInRange.size(); i++) {
                    Cylinder cylinder = cylindersInRange.get(i);
                    int distance = cylinder.getDistanceFrom(currentHeadLocation);
                    int minDistance = nextCylinder.getDistanceFrom(currentHeadLocation);

                    if (distance < minDistance) {
                        nextCylinder = cylinder;
                    }
                }
            }

            // 현재 scan 방향에 요청 실린더가 없다면, 반대편 방향에서 가장 먼 요청 실린더를 추출
            if (nextCylinder == null && !cylindersOutRange.isEmpty()) {
                nextCylinder = cylindersOutRange.get(0);
                for (int i = 1; i < cylindersOutRange.size(); i++) {
                    Cylinder cylinder = cylindersOutRange.get(i);
                    int distance = cylinder.getDistanceFrom(currentHeadLocation);
                    int maxDistance = nextCylinder.getDistanceFrom(currentHeadLocation);

                    if (distance > maxDistance) {
                        nextCylinder = cylinder;
                    }
                }
            }
        }

        cylinders.remove(nextCylinder);

        return nextCylinder;
    }

    private boolean isCylinderInRange(Cylinder cylinder, int currentCylinderNumber, ScanDirection currentScanDirection) {
        return (currentScanDirection.equals(ScanDirection.LEFT) && cylinder.isNumberLessOrEqualsFrom(currentCylinderNumber))
                || (currentScanDirection.equals(ScanDirection.RIGHT) && cylinder.isNumberGreaterOrEqualsFrom(currentCylinderNumber));
    }

    public Cylinders getSameCylindersFrom(int cylinderNumber) {
        Cylinders sameCylinders = Cylinders.create();

        for (int i = 0; i < cylinders.size(); i++) {
            Cylinder cylinder = cylinders.get(i);
            if (cylinder.hasSameNumberAs(cylinderNumber)) {
                sameCylinders.addToBack(cylinder);
                cylinders.remove(i);
                i--;
            }
        }

        return sameCylinders;
    }

    @Override
    public void increaseWaitingTime() {
        cylinders.forEach(Cylinder::increaseWaitingTime);
    }
}
