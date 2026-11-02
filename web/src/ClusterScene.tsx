import { useEffect, useRef, useState } from 'react';
import * as THREE from 'three';
import { OrbitControls } from 'three/addons/controls/OrbitControls.js';
import type { Worker } from './engine';

type Props = {
  workers: Worker[];
  active: boolean;
  selected: number | null;
  onSelect: (shard: number | null) => void;
  paused: boolean;
  online: boolean;
};
/** Visual topology only: status comes from HTTP diagnostics; motion is an activity cue, not telemetry. */
export default function ClusterScene(props: Props) {
  const mount = useRef<HTMLDivElement>(null),
    latest = useRef(props);
  const invalidate = useRef<(() => void) | null>(null);
  const [unavailable, setUnavailable] = useState(false);
  latest.current = props;
  useEffect(() => {
    const element = mount.current!;
    let renderer: THREE.WebGLRenderer;
    try {
      renderer = new THREE.WebGLRenderer({
        antialias: true,
        alpha: true,
        powerPreference: 'low-power',
      });
    } catch {
      setUnavailable(true);
      return;
    }
    renderer.setPixelRatio(Math.min(devicePixelRatio, 1.5));
    renderer.outputColorSpace = THREE.SRGBColorSpace;
    renderer.toneMapping = THREE.ACESFilmicToneMapping;
    renderer.toneMappingExposure = 1.3;
    element.appendChild(renderer.domElement);
    renderer.domElement.setAttribute(
      'aria-label',
      'Interactive 3D cluster: drag to orbit, scroll to zoom, click a worker to inspect',
    );
    renderer.domElement.setAttribute('role', 'img');
    const scene = new THREE.Scene();
    const camera = new THREE.PerspectiveCamera(36, 1, 0.1, 70);
    camera.position.set(7.3, 6.5, 9.3);
    const controls = new OrbitControls(camera, renderer.domElement);
    controls.target.set(0, 0.2, 0);
    controls.enableDamping = true;
    controls.enablePan = false;
    controls.minDistance = 7;
    controls.maxDistance = 18;
    controls.maxPolarAngle = Math.PI * 0.47;
    controls.minPolarAngle = 0.25;
    controls.autoRotateSpeed = 0.3;
    scene.add(new THREE.AmbientLight(0xe0dfd1, 2));
    const key = new THREE.DirectionalLight(0xfff5df, 3);
    key.position.set(3, 8, 4);
    scene.add(key);
    const rim = new THREE.PointLight(0xb29a77, 12, 20);
    rim.position.set(-4, 3, -3);
    scene.add(rim);
    const cyan = new THREE.PointLight(0xc8cebc, 10, 16);
    cyan.position.set(4, 2, 4);
    scene.add(cyan);
    const world = new THREE.Group();
    scene.add(world);
    const core = new THREE.Group();
    core.position.y = 0.75;
    world.add(core);
    const crystal = new THREE.Mesh(
      new THREE.IcosahedronGeometry(0.56, 0),
      new THREE.MeshPhysicalMaterial({
        color: 0xbe9470,
        metalness: 0.35,
        roughness: 0.45,
        clearcoat: 0,
        emissive: 0x3b2416,
        emissiveIntensity: 0.15,
      }),
    );
    core.add(crystal);
    const cage = new THREE.LineSegments(
      new THREE.EdgesGeometry(new THREE.IcosahedronGeometry(0.75, 0)),
      new THREE.LineBasicMaterial({
        color: 0xb5b2a1,
        transparent: true,
        opacity: 0.45,
      }),
    );
    core.add(cage);
    const anchor = new THREE.Mesh(
      new THREE.CylinderGeometry(0.55, 0.7, 0.12, 48),
      new THREE.MeshStandardMaterial({
        color: 0x4b5046,
        metalness: 0.3,
        roughness: 0.65,
      }),
    );
    anchor.position.y = -0.83;
    world.add(anchor);
    const positions = [
      new THREE.Vector3(-2.8, 0, 1.45),
      new THREE.Vector3(2.65, 0, 1.5),
      new THREE.Vector3(0.1, 0, -2.8),
    ];
    const nodes: THREE.Group[] = [],
      lights: THREE.MeshBasicMaterial[] = [],
      bodies: THREE.MeshStandardMaterial[] = [];
    const clickable: THREE.Object3D[] = [];
    positions.forEach((position, shard) => {
      const group = new THREE.Group();
      group.position.copy(position);
      world.add(group);
      nodes.push(group);
      const body = new THREE.MeshStandardMaterial({
        color: 0x777d70,
        metalness: 0.4,
        roughness: 0.5,
        emissive: 0x181c15,
        emissiveIntensity: 0.2,
      });
      bodies.push(body);
      const light = new THREE.MeshBasicMaterial({ color: 0x657b96 });
      lights.push(light);
      for (let layer = 0; layer < 4; layer++) {
        const box = new THREE.Mesh(
          new THREE.BoxGeometry(1.18, 0.22, 0.9),
          body,
        );
        box.position.y = layer * 0.29 - 0.45;
        box.userData.shard = shard;
        group.add(box);
        clickable.push(box);
        const edge = new THREE.LineSegments(
          new THREE.EdgesGeometry(box.geometry),
          new THREE.LineBasicMaterial({
            color: 0xbec3b0,
            transparent: true,
            opacity: 0.3,
          }),
        );
        edge.position.copy(box.position);
        group.add(edge);
        const strip = new THREE.Mesh(
          new THREE.BoxGeometry(0.62, 0.024, 0.012),
          light,
        );
        strip.position.set(-0.14, box.position.y, 0.458);
        group.add(strip);
        for (let led = 0; led < 2; led++) {
          const dot = new THREE.Mesh(
            new THREE.SphereGeometry(0.018, 6, 6),
            light,
          );
          dot.position.set(0.34 + led * 0.1, box.position.y, 0.46);
          group.add(dot);
        }
      }
      const pedestal = new THREE.Mesh(
        new THREE.CylinderGeometry(0.95, 1.05, 0.13, 6),
        new THREE.MeshStandardMaterial({
          color: 0x484e42,
          metalness: 0.25,
          roughness: 0.65,
        }),
      );
      pedestal.position.y = -0.78;
      group.add(pedestal);
      const path = new THREE.CatmullRomCurve3([
        new THREE.Vector3(0, 0.5, 0),
        position
          .clone()
          .multiplyScalar(0.5)
          .add(new THREE.Vector3(0, 0.8, 0)),
        position.clone().add(new THREE.Vector3(0, 0.35, 0)),
      ]);
      const link = new THREE.Mesh(
        new THREE.TubeGeometry(path, 20, 0.012, 5, false),
        new THREE.MeshBasicMaterial({
          color: 0x969e89,
          transparent: true,
          opacity: 0.5,
        }),
      );
      world.add(link);
    });
    let dirty = true,
      frame: number | null = null,
      last = performance.now(),
      time = 0,
      lastProps: Props | null = null;
    function requestRender() {
      if (frame === null) frame = requestAnimationFrame(animate);
    }
    const resize = new ResizeObserver(() => {
      const width = element.clientWidth,
        height = element.clientHeight;
      if (!width || !height) return;
      renderer.setSize(width, height);
      camera.aspect = width / height;
      camera.updateProjectionMatrix();
      dirty = true;
      requestRender();
    });
    resize.observe(element);
    const ray = new THREE.Raycaster(),
      pointer = new THREE.Vector2();
    let down: [number, number] = [0, 0];
    const point = (event: PointerEvent) => {
      const rect = renderer.domElement.getBoundingClientRect();
      pointer.set(
        ((event.clientX - rect.left) / rect.width) * 2 - 1,
        (-(event.clientY - rect.top) / rect.height) * 2 + 1,
      );
      ray.setFromCamera(pointer, camera);
      return ray.intersectObjects(clickable)[0];
    };
    const onDown = (event: PointerEvent) => {
      down = [event.clientX, event.clientY];
    };
    const onUp = (event: PointerEvent) => {
      if (Math.hypot(event.clientX - down[0], event.clientY - down[1]) > 5)
        return;
      const hit = point(event);
      latest.current.onSelect(hit ? Number(hit.object.userData.shard) : null);
    };
    const onMove = (event: PointerEvent) => {
      element.style.cursor = point(event) ? 'pointer' : 'grab';
    };
    renderer.domElement.addEventListener('pointerdown', onDown);
    renderer.domElement.addEventListener('pointerup', onUp);
    renderer.domElement.addEventListener('pointermove', onMove);
    const reduced = matchMedia('(prefers-reduced-motion: reduce)');
    function animate(now: number) {
      frame = null;
      if (now - last < 32) {
        requestRender();
        return;
      }
      const delta = Math.min((now - last) / 1000, 0.1);
      last = now;
      if (document.hidden) return;
      const { workers, active, selected, paused, online } = latest.current;
      const motion = active && !paused && !reduced.matches;
      if (motion) time += delta;
      controls.autoRotate = motion && selected === null;
      const controlsChanged = controls.update(delta);
      crystal.rotation.y = time * 0.17;
      crystal.rotation.z = 0.15 + Math.sin(time * 0.2) * 0.1;
      cage.rotation.y = -time * 0.1;
      core.position.y = 0.75;
      crystal.material.emissiveIntensity = online
        ? active
          ? 0.3
          : 0.15
        : 0.04;
      let settling = false;
      nodes.forEach((node, index) => {
        const state = workers.find((worker) => worker.shard === index)?.status;
        const color =
          state === 'ready'
            ? 0xa6cfad
            : state === 'unavailable'
              ? 0xe8ad7b
              : 0x9ba591;
        lights[index].color.setHex(color);
        bodies[index].emissive.setHex(selected === index ? 0x51361e : 0x181c15);
        node.position.y = 0;
        const scale = selected === index ? 1.12 : 1;
        if (Math.abs(node.scale.x - scale) > 0.001) {
          node.scale.lerp(
            new THREE.Vector3(scale, scale, scale),
            reduced.matches ? 1 : 0.2,
          );
          settling = true;
        }
      });
      if (
        dirty ||
        controlsChanged ||
        settling ||
        motion ||
        lastProps !== latest.current
      ) {
        renderer.render(scene, camera);
        dirty = false;
        lastProps = latest.current;
      }
      if (controlsChanged || settling || motion) requestRender();
    }
    const onVisibility = () => {
      if (!document.hidden) {
        dirty = true;
        requestRender();
      }
    };
    const onReduced = () => {
      dirty = true;
      requestRender();
    };
    document.addEventListener('visibilitychange', onVisibility);
    reduced.addEventListener('change', onReduced);
    controls.addEventListener('change', requestRender);
    invalidate.current = requestRender;
    requestRender();
    const onLoss = (event: Event) => {
      event.preventDefault();
      setUnavailable(true);
    };
    renderer.domElement.addEventListener('webglcontextlost', onLoss);
    return () => {
      if (frame !== null) cancelAnimationFrame(frame);
      invalidate.current = null;
      resize.disconnect();
      controls.removeEventListener('change', requestRender);
      controls.dispose();
      document.removeEventListener('visibilitychange', onVisibility);
      reduced.removeEventListener('change', onReduced);
      renderer.domElement.removeEventListener('pointerdown', onDown);
      renderer.domElement.removeEventListener('pointerup', onUp);
      renderer.domElement.removeEventListener('pointermove', onMove);
      renderer.domElement.removeEventListener('webglcontextlost', onLoss);
      const geometries = new Set<THREE.BufferGeometry>(),
        materials = new Set<THREE.Material>();
      scene.traverse((object) => {
        if (
          object instanceof THREE.Mesh ||
          object instanceof THREE.LineSegments
        ) {
          geometries.add(object.geometry);
          const list = Array.isArray(object.material)
            ? object.material
            : [object.material];
          list.forEach((material) => materials.add(material));
        }
      });
      geometries.forEach((geometry) => geometry.dispose());
      materials.forEach((material) => material.dispose());
      renderer.dispose();
      element.replaceChildren();
    };
  }, []);
  useEffect(() => {
    invalidate.current?.();
  }, [props.workers, props.active, props.selected, props.paused, props.online]);
  return (
    <div className="scene-canvas" ref={mount}>
      {unavailable && (
        <div className="scene-fallback">
          <b>3D rendering unavailable</b>
          <p>Search and live worker diagnostics remain available below.</p>
        </div>
      )}
    </div>
  );
}
