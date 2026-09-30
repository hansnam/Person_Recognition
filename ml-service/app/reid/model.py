"""
Model Architecture for CUHK03 ResNet-50 Re-ID with BNNeck.
Backbone: ResNet-50 with layer4 stride=(1, 1).
Neck: Global Average Pooling (GAP) + BatchNorm1d(2048).
Output: 2048-dim feature vector.
"""

import os
import torch
import torch.nn as nn
import torchvision.models as models


class BaselineReID(nn.Module):
    def __init__(self, num_classes: int = 767):
        super().__init__()
        # Load torchvision ResNet-50 without pretrained weights
        resnet = models.resnet50(weights=None)
        
        # Modify layer4 stride to (1, 1) to retain spatial details
        resnet.layer4[0].conv2.stride = (1, 1)
        resnet.layer4[0].downsample[0].stride = (1, 1)

        # Base backbone: conv1 to layer4 (children 0 to 7)
        self.base = nn.Sequential(*list(resnet.children())[:-2])
        self.gap = nn.AdaptiveAvgPool2d(1)
        self.bottleneck = nn.BatchNorm1d(2048)
        self.bottleneck.bias.requires_grad_(False)
        self.classifier = nn.Linear(2048, num_classes, bias=False)

    def forward(self, x: torch.Tensor) -> torch.Tensor:
        feat = self.base(x)
        global_feat = self.gap(feat)
        global_feat = global_feat.view(global_feat.shape[0], -1)
        bn_feat = self.bottleneck(global_feat)
        if self.training:
            cls_score = self.classifier(bn_feat)
            return cls_score, global_feat
        return bn_feat


def load_reid_model(weights_path: str, device: str = "cpu") -> nn.Module:
    """
    Khởi tạo và nạp trọng số checkpoint best_cuhk03_model_rerank.pth
    """
    if not os.path.exists(weights_path):
        raise FileNotFoundError(f"Không tìm thấy file trọng số Re-ID: {weights_path}")

    model = BaselineReID()
    checkpoint = torch.load(weights_path, map_location=device)

    if isinstance(checkpoint, dict) and "state_dict" in checkpoint:
        state_dict = checkpoint["state_dict"]
    elif isinstance(checkpoint, dict) and "model" in checkpoint:
        state_dict = checkpoint["model"]
    else:
        state_dict = checkpoint

    model.load_state_dict(state_dict, strict=True)
    model.to(device)
    model.eval()
    print(f"[ReID Model] Da nap thanh cong mo hinh CUHK03 ResNet-50 Re-ID tu: {weights_path}")
    return model


load_cuhk03_model = load_reid_model

